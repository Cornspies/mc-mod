#!/usr/bin/env python3
"""Synthesizes every sound effect of the mod and writes sounds.json (DESIGN.md §4.4).

All sounds are made here from oscillators, noise, filters and envelopes, so they are our own (MIT) and need no
attribution. Each sound is a function below; the EVENTS table at the end maps sound events to them, with the
number of variants (Minecraft picks one at random per play), the volume and the distance at which they fade out.

- weapons: an energy weapon shot is a sharp crack, a saturated noise body, a low thump, a fast falling "zap" (the
  fusion part), an electric sizzle and an outdoor tail. Each weapon has its own numbers (GUNS).
- explosions: crack, sub-bass drop, saturated boom, a long rumble and debris crackle.
- tones (lock-on, warnings, radar): short soft-edged tones; the shooter's lock tones are sines, the target's
  warnings harsher squares, so the two never sound alike.
- loops (machines, generator, charging station, rocket in flight): built circularly, every noise and modulation
  repeats exactly after the loop length, so they loop without a seam.

Needs numpy and ffmpeg with libvorbis. Writes mono Ogg Vorbis (positional sounds must be mono) to
assets/fusionfactions/sounds/ and assets/fusionfactions/sounds.json. Edit this script, not the files.

Run from the repository root:  python3 tools/sound_effects.py
"""
import json
import subprocess
import zlib
from pathlib import Path

import numpy as np

SR = 44100
ROOT = Path('src/main/resources/assets/fusionfactions')
SOUNDS = ROOT / 'sounds'


# --- building blocks ---------------------------------------------------------------------------------------------

def n(seconds):
    return int(round(seconds * SR))


def time(seconds):
    return np.arange(n(seconds)) / SR


def lp(fc, order=2):
    return lambda f: 1.0 / np.sqrt(1.0 + (f / fc) ** (2 * order))


def hp(fc, order=2):
    return lambda f: 1.0 / np.sqrt(1.0 + (fc / np.maximum(f, 1e-3)) ** (2 * order))


def bp(fc, q=1.0):
    return lambda f: 1.0 / np.sqrt(1.0 + (q * (f / fc - fc / np.maximum(f, 1e-3))) ** 2)


def filt(x, *responses):
    """Zero-phase filtering in the frequency domain. Circular, so filtered loops stay seamless. Filter sources
    before giving them an envelope, so attacks stay sharp."""
    spectrum = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1.0 / SR)
    for response in responses:
        spectrum = spectrum * response(f)
    return np.fft.irfft(spectrum, len(x))


def tv_filter(x, response_at, frame=2048, hop=256):
    """A filter that changes over time: response_at(t) gives the response for the frame around t seconds."""
    window = np.hanning(frame + 1)[:-1]
    padded = np.concatenate([np.zeros(frame), x, np.zeros(frame)])
    out = np.zeros_like(padded)
    norm = np.zeros_like(padded)
    f = np.fft.rfftfreq(frame, 1.0 / SR)
    for start in range(0, len(padded) - frame, hop):
        t = (start + frame / 2 - frame) / SR
        y = np.fft.irfft(np.fft.rfft(padded[start:start + frame] * window) * response_at(t)(f), frame) * window
        out[start:start + frame] += y
        norm[start:start + frame] += window ** 2
    return (out / np.maximum(norm, 1e-6))[frame:frame + len(x)]


def noise(rng, seconds, slope=0.0, *responses):
    """Noise with a spectral slope (0 white, 1 pink, 2 brown), shaped by filters. Generated circularly."""
    m = n(seconds)
    spectrum = rng.standard_normal(m // 2 + 1) + 1j * rng.standard_normal(m // 2 + 1)
    f = np.fft.rfftfreq(m, 1.0 / SR)
    spectrum *= np.maximum(f, 1.0) ** (-slope / 2.0)
    spectrum[0] = 0.0
    if slope > 0:
        # Pink and brown noise would otherwise carry most of their energy below hearing, as slow swings.
        spectrum *= hp(30, 2)(f)
    for response in responses:
        spectrum = spectrum * response(f)
    x = np.fft.irfft(spectrum, m)
    return x / (np.std(x) + 1e-12)


def wobble(rng, seconds, rate):
    """A slow random modulation around 0 (roughly -1..1) that changes about `rate` times a second."""
    x = noise(rng, seconds, 0.0, lp(rate, 2))
    return x / (np.max(np.abs(x)) + 1e-12)


def phase(freq, m):
    return 2.0 * np.pi * np.cumsum(np.broadcast_to(np.asarray(freq, dtype=float), (m,))) / SR


def sine(freq, seconds, start_phase=0.0):
    m = n(seconds)
    return np.sin(phase(freq, m) + start_phase)


def wave(freq, seconds, kind='saw', max_freq=15000.0):
    """Band-limited saw, square or triangle (harmonics fade out below max_freq, so sweeps don't alias)."""
    m = n(seconds)
    ph = phase(freq, m)
    f0 = np.broadcast_to(np.asarray(freq, dtype=float), (m,))
    out = np.zeros(m)
    for k in range(1, 200):
        if kind != 'saw' and k % 2 == 0:
            continue
        weight = np.clip((max_freq - k * f0) / 2000.0, 0.0, 1.0)
        if not weight.any():
            break
        if kind == 'tri':
            amp = (1.0 if (k // 2) % 2 == 0 else -1.0) / (k * k)
        else:
            amp = 1.0 / k
        out += amp * weight * np.sin(k * ph)
    return out / (np.max(np.abs(out)) + 1e-12)


def glide(f0, f1, tau, seconds):
    """A frequency that moves from f0 towards f1 with time constant tau (the "pew" curve)."""
    return f1 + (f0 - f1) * np.exp(-time(seconds) / tau)


def env(seconds, attack=0.001, decay=0.1, hold=0.0):
    """Linear attack, then hold, then exponential decay."""
    t = time(seconds)
    a = np.clip(t / attack, 0.0, 1.0) if attack > 0 else 1.0
    d = np.where(t < attack + hold, 1.0, np.exp(-(t - attack - hold) / decay))
    return a * d


def gate(seconds, attack=0.005, release=0.01):
    """Flat, with soft raised-cosine edges."""
    t = time(seconds)
    a = 0.5 - 0.5 * np.cos(np.pi * np.clip(t / attack, 0.0, 1.0))
    r = 0.5 - 0.5 * np.cos(np.pi * np.clip((seconds - t) / release, 0.0, 1.0))
    return a * r


def metal(rng, freqs, decays, seconds):
    """A struck metal part: inharmonic partials, each with its own decay."""
    out = np.zeros(n(seconds))
    for i, (f, d) in enumerate(zip(freqs, decays)):
        out += sine(f * rng.uniform(0.98, 1.02), seconds, rng.uniform(0, 2 * np.pi)) * env(seconds, 0.0005, d) / (i + 1) ** 0.5
    return out


def click(rng, center=3000.0, decay=0.003, q=0.8, seconds=0.03):
    return noise(rng, seconds, 0.0, bp(center, q)) * env(seconds, 0.0002, decay)


def thump(f0, f1, decay, seconds=None, tau=0.03):
    seconds = seconds or decay * 6
    return sine(glide(f0, f1, tau, seconds), seconds) * env(seconds, 0.001, decay)


def tone(freq, seconds, kind='sine', attack=0.004, release=0.02, brightness=1.0):
    if kind == 'sine':
        x = sine(freq, seconds) + 0.18 * brightness * sine(3 * freq, seconds)
    else:
        x = wave(freq, seconds, kind, max_freq=freq * 9 * brightness)
    return x * gate(seconds, attack, release)


def fftconvolve(a, b):
    size = len(a) + len(b) - 1
    nfft = 1 << (size - 1).bit_length()
    return np.fft.irfft(np.fft.rfft(a, nfft) * np.fft.rfft(b, nfft), nfft)[:size]


def reverb(rng, x, rt60, mix, tone_hz=3000.0, predelay=0.012):
    """A plain outdoor-ish tail: the dry sound plus its convolution with decaying, darkened noise (without the
    lowest frequencies, which would make it boom)."""
    length = rt60 * 1.1
    ir = rng.standard_normal(n(length)) * np.exp(-6.9 * time(length) / rt60)
    ir = filt(ir, lp(tone_hz, 1), hp(150, 1))
    ir[:n(predelay)] = 0.0
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-12
    wet = fftconvolve(x, ir)
    dry = np.concatenate([x, np.zeros(len(wet) - len(x))])
    return dry + mix * wet


def sat(x, drive):
    """Soft clipping, for punch: normalizes, then tanh."""
    x = x / (np.max(np.abs(x)) + 1e-12)
    return np.tanh(drive * x) / np.tanh(drive)


class Track:
    """A buffer that layers are added into; a looping track wraps what runs past its end around to the start."""

    def __init__(self, seconds, loop=False):
        self.buf = np.zeros(n(seconds))
        self.loop = loop

    def add(self, signal, at=0.0, gain=1.0):
        start = n(at)
        if self.loop:
            idx = (start + np.arange(len(signal))) % len(self.buf)
            np.add.at(self.buf, idx, signal * gain)
        else:
            end = min(len(self.buf), start + len(signal))
            if end > start:
                self.buf[start:end] += signal[:end - start] * gain
        return self


def periodic(freq, seconds):
    """A sine at a frequency rounded so that it repeats exactly in `seconds` (for loops)."""
    cycles = max(1, round(freq * seconds))
    return np.sin(2 * np.pi * cycles * time(seconds) / seconds)


# --- weapons -----------------------------------------------------------------------------------------------------

GUNS = {
    # length, crack (highpass, decay), body (center, decay), thump (from, to, decay), zap (from, to, tau, decay),
    # sizzle decay, layer gains, drive, tail (rt60, mix, tone)
    'pistol': dict(length=0.9, crack=(2000, 0.006), body=(900, 0.035), thump=(160, 55, 0.07), zap=(2600, 220, 0.030, 0.09),
                   sizzle=0.12, gains=(0.8, 1.0, 0.9, 0.35, 0.15), drive=2.5, tail=(0.8, 0.35, 3500)),
    'auto_pistol': dict(length=0.7, crack=(2200, 0.005), body=(1100, 0.030), thump=(180, 65, 0.05), zap=(3000, 300, 0.025, 0.07),
                        sizzle=0.10, gains=(0.8, 1.0, 0.75, 0.35, 0.15), drive=2.5, tail=(0.6, 0.3, 3500)),
    'smg': dict(length=0.6, crack=(2400, 0.004), body=(1300, 0.025), thump=(200, 70, 0.04), zap=(3400, 380, 0.020, 0.06),
                sizzle=0.08, gains=(0.85, 1.0, 0.65, 0.35, 0.15), drive=2.5, tail=(0.5, 0.3, 3800)),
    'shotgun': dict(length=1.4, crack=(1200, 0.008), body=(600, 0.080), thump=(120, 40, 0.14), zap=(1800, 120, 0.050, 0.14),
                    sizzle=0.20, gains=(0.8, 1.0, 1.2, 0.3, 0.15), drive=3.2, tail=(1.2, 0.4, 3000)),
    'sniper_rifle': dict(length=2.2, crack=(2500, 0.010), body=(800, 0.060), thump=(110, 35, 0.16), zap=(4200, 160, 0.045, 0.20),
                         sizzle=0.30, gains=(1.0, 1.0, 1.1, 0.4, 0.2), drive=3.0, tail=(2.0, 0.5, 2500)),
    'gatling_gun': dict(length=0.35, crack=(2400, 0.004), body=(1400, 0.018), thump=(220, 80, 0.03), zap=(3200, 500, 0.015, 0.04),
                        sizzle=0.05, gains=(0.8, 1.0, 0.6, 0.3, 0.1), drive=2.2, tail=(0.25, 0.2, 4000)),
}


def gunshot(rng, p):
    d = rng.uniform(0.97, 1.03)
    crack_hp, crack_decay = p['crack']
    body_f, body_decay = p['body']
    thump_from, thump_to, thump_decay = p['thump']
    zap_from, zap_to, zap_tau, zap_decay = p['zap']
    g_crack, g_body, g_thump, g_zap, g_sizzle = p['gains']
    s = 0.5
    crack = noise(rng, s, 0.0, hp(crack_hp * d, 2)) * env(s, 0.0003, crack_decay)
    body = noise(rng, s, 0.0, bp(body_f * d, 0.7)) * env(s, 0.0008, body_decay)
    low = thump(thump_from * d, thump_to * d, thump_decay, s)
    zf = glide(zap_from * d, zap_to * d, zap_tau, s)
    zap = filt(wave(zf, s) + 0.6 * wave(zf * 1.007, s), lp(5000, 2)) * env(s, 0.001, zap_decay)
    crackle = 0.3 + 0.7 * np.abs(noise(rng, s, 0.0, lp(150, 2))) / 2.5
    sizzle = noise(rng, s, 0.0, bp(5500, 1.2)) * crackle * env(s, 0.002, p['sizzle'])
    # The zap is added after the saturation, so the noise doesn't bury it.
    dry = sat(g_crack * crack + g_body * body + g_thump * low, p['drive']) + g_zap * zap + g_sizzle * sizzle
    rt60, mix, tone_hz = p['tail']
    out = Track(p['length']).add(reverb(rng, dry, rt60, mix, tone_hz)).buf
    return out


def rocket_launcher_fire(rng):
    s = 1.8
    bang = sat(noise(rng, s, 0.0, hp(1000, 2)) * env(s, 0.0003, 0.01) + thump(90, 35, 0.15, s), 2.5)
    # The whoosh's band rises from 500 to 2800 Hz in 0.4 s, then falls back towards 1500 Hz.
    band = lambda t: bp(500 + 2300 * min(t / 0.4, 1.0) - 1300 * max(0.0, t - 0.4) / 1.4, 0.9)
    whoosh = tv_filter(noise(rng, s), band) * env(s, 0.03, 0.35, hold=0.25)
    backblast = noise(rng, s, 2.0, lp(900, 2)) * env(s, 0.005, 0.25)
    dry = 0.9 * bang + 0.8 * whoosh + 0.7 * backblast
    return Track(2.6).add(reverb(rng, dry, 1.2, 0.35, 2500)).buf


def gatling_spin(rng):
    s = 1.3
    t = time(s)
    f = 70 + 520 * (1 - np.exp(-t / 0.45))
    whine = filt(wave(f, s), lp(2500, 2)) * 0.35 + sine(f * 4, s) * 0.12
    rate = 6 + 34 * np.clip(t / 1.0, 0, 1) ** 0.8
    ticks = np.floor(np.cumsum(rate) / SR)
    rattle = Track(s)
    for i in np.nonzero(np.diff(ticks))[0]:
        rattle.add(click(rng, rng.uniform(1600, 2600), 0.004), i / SR, rng.uniform(0.15, 0.3))
    fade = np.clip((s - t) / 0.25, 0, 1)
    return (whine + rattle.buf) * np.clip(t / 0.05, 0, 1) * fade


def weapon_empty(rng):
    out = Track(0.15)
    for at, g in ((0.0, 1.0), (0.03, 0.7)):
        out.add(click(rng, 3500, 0.002, 0.6), at, g)
        out.add(sine(3800, 0.05) * env(0.05, 0.0005, 0.015), at, 0.35 * g)
    out.add(sine(glide(700, 250, 0.03, 0.08), 0.08) * env(0.08, 0.002, 0.04), 0.035, 0.25)
    return out.buf


def weapon_reload(rng):
    out = Track(0.2)
    out.add(click(rng, rng.uniform(2200, 2800), 0.003), 0.0, 1.0)
    out.add(thump(190, 150, 0.025, 0.12), 0.0, 0.8)
    out.add(metal(rng, (1700, 3100), (0.03, 0.02), 0.12), 0.0, 0.3)
    out.add(click(rng, 3000, 0.002), 0.045, 0.5)
    out.add(sine(glide(450, 1100, 0.03, 0.1), 0.1) * env(0.1, 0.005, 0.04), 0.06, 0.25)
    return out.buf


def weapon_reload_done(rng):
    out = Track(0.65)
    out.add(click(rng, 2400, 0.004), 0.0, 1.0)
    out.add(thump(160, 120, 0.05, 0.25), 0.0, 1.0)
    out.add(click(rng, 3200, 0.003), 0.05, 0.7)
    out.add(thump(220, 160, 0.03, 0.15), 0.05, 0.5)
    t = time(0.32)
    out.add(sine(300 + 1500 * (t / 0.32) ** 1.5, 0.32) * np.clip(t / 0.25, 0, 1) * gate(0.32, 0.01, 0.03), 0.08, 0.3)
    out.add(sine(1800, 0.25) * env(0.25, 0.001, 0.1) + 0.4 * sine(3600, 0.25) * env(0.25, 0.001, 0.05), 0.4, 0.35)
    return out.buf


def hit_marker(rng):
    out = Track(0.08)
    out.add(noise(rng, 0.08, 0.0, hp(3000, 2)) * env(0.08, 0.0002, 0.003), 0.0, 0.6)
    out.add(sine(2300, 0.08) * env(0.08, 0.0005, 0.02), 0.0, 0.5)
    out.add(sine(1500, 0.08) * env(0.08, 0.0005, 0.012), 0.0, 0.3)
    return out.buf


def headshot(rng):
    out = Track(0.45).add(hit_marker(rng))
    out.add(noise(rng, 0.1, 0.0, bp(1200, 1.0)) * env(0.1, 0.0005, 0.02), 0.0, 0.5)
    out.add(sine(2650, 0.45) * env(0.45, 0.001, 0.18), 0.005, 0.6)
    out.add(sine(2650 * 2.76, 0.45) * env(0.45, 0.001, 0.08), 0.005, 0.3)
    out.add(sine(1325, 0.45) * env(0.45, 0.001, 0.25), 0.005, 0.25)
    return out.buf


# --- explosions --------------------------------------------------------------------------------------------------

def explosion(rng, length, sub, boom, rumble, debris, drive, tail):
    """sub = (from, to, decay), boom = (lowpass, decay), rumble = (lowpass, decay, gain), debris = count,
    tail = (rt60, mix)."""
    crack = noise(rng, length, 0.0, hp(700, 2)) * env(length, 0.0005, 0.02)
    low = thump(sub[0], sub[1], sub[2], length, tau=0.15)
    body = noise(rng, length, 1.0, lp(boom[0], 2)) * env(length, 0.002, boom[1])
    roll = noise(rng, length, 2.0, lp(rumble[0], 2)) * (0.7 + 0.3 * wobble(rng, length, 3)) * env(length, 0.05, rumble[1])
    bits = Track(length)
    for _ in range(debris):
        at = rng.exponential(length * 0.25)
        if at < length - 0.05:
            bits.add(click(rng, rng.uniform(1500, 5000), rng.uniform(0.003, 0.008), 2.0), at, rng.uniform(0.2, 1.0) * np.exp(-at / length * 2))
    dry = sat(0.9 * crack + 1.0 * low + 1.0 * body, drive) + rumble[2] * roll / (np.max(np.abs(roll)) + 1e-12) + 0.25 * bits.buf
    return Track(length).add(reverb(rng, dry, tail[0], tail[1], 1500)).buf


def missile_explode(rng):
    return explosion(rng, 5.0, (55, 28, 0.6), (900, 1.2), (220, 1.4, 0.6), 60, 3.0, (2.5, 0.4))


def rocket_explode(rng):
    return explosion(rng, 2.2, (100, 40, 0.2), (2500, 0.25), (300, 0.5, 0.4), 20, 2.5, (1.2, 0.35))


def missile_shot_down(rng):
    burst = explosion(rng, 3.0, (120, 50, 0.15), (1800, 0.3), (300, 0.6, 0.35), 25, 2.5, (1.5, 0.35))
    out = Track(3.0).add(burst)
    out.add(filt(burst, lp(1500, 2)), 0.45, 0.35)
    out.add(filt(burst, lp(900, 2)), 1.1, 0.2)
    return out.buf


def missile_emp(rng):
    s = 3.0
    t = time(s)
    out = Track(s)
    out.add(thump(90, 32, 0.35, s, tau=0.2), 0.0, 1.0)
    spikes = (rng.random(n(s)) < 0.004).astype(float) * rng.uniform(0.3, 1.0, n(s))
    spiky = fftconvolve(spikes, np.exp(-time(0.004) / 0.0008))[:n(s)]
    out.add(noise(rng, s, 0.0, bp(3500, 0.8)) * spiky * env(s, 0.002, 0.5), 0.0, 0.9)
    out.add(filt(wave(glide(2400, 400, 0.04, 0.4), 0.4), lp(6000, 2)) * env(0.4, 0.001, 0.08), 0.0, 0.5)
    down = wave(900 * np.exp(-t / 0.6) + 45, s)
    down = tv_filter(down, lambda tt: lp(max(200.0, 4000.0 * np.exp(-tt / 0.7)), 2)) * env(s, 0.05, 1.0)
    out.add(down, 0.0, 0.45)
    hum = filt(wave(50 - 15 * t / s, s), lp(400, 2)) * env(s, 0.01, 1.5)
    out.add(hum, 0.0, 0.3)
    return Track(3.8).add(reverb(rng, out.buf, 1.8, 0.35, 2500)).buf


# --- missiles, rockets, interceptor ------------------------------------------------------------------------------

def missile_launch(rng):
    s = 6.0
    t = time(s)
    ignition = sat(noise(rng, s, 0.0, hp(900, 2)) * env(s, 0.0005, 0.015) + thump(70, 30, 0.3, s), 2.5)
    source = noise(rng, s, 2.0) + 0.25 * noise(rng, s)
    roar = tv_filter(source, lambda tt: lp(max(500.0, 3500.0 * np.exp(-max(tt - 0.3, 0.0) / 2.5)), 2))
    roar *= 1 + 0.25 * wobble(rng, s, 25)
    shape = np.clip(t / 0.25, 0, 1) * np.where(t < 2.5, 1.0, np.exp(-(t - 2.5) / 1.2))
    crackle = Track(s)
    for _ in range(120):
        at = rng.exponential(1.2)
        if at < s - 0.05:
            crackle.add(click(rng, rng.uniform(2000, 4000), 0.003, 2.0), at, rng.uniform(0.2, 0.6) * np.exp(-at / 1.5))
    dry = 0.9 * ignition + 0.8 * roar / np.max(np.abs(roar)) * shape + 0.4 * crackle.buf
    return Track(s).add(reverb(rng, dry, 2.5, 0.4, 2000)).buf


def missile_incoming(rng):
    s = 4.0
    t = time(s)
    f = 520 + 1380 * np.exp(-t / 2.2)
    whistle = sine(f * (1 + 0.006 * np.sin(2 * np.pi * 6 * t)), s) + 0.3 * sine(2 * f, s)
    whistle *= 0.2 + 0.8 * (t / s) ** 1.5
    roar = tv_filter(noise(rng, s, 2.0) + 0.3 * noise(rng, s), lambda tt: lp(400 + 4600 * (tt / s) ** 2, 2))
    roar = roar / np.max(np.abs(roar)) * (t / s) ** 2.5
    out = 0.45 * whistle + 0.8 * roar
    return out * np.clip((s - t) / 0.03, 0, 1)


def interceptor_launch(rng):
    s = 1.2
    pop = sat(noise(rng, s, 0.0, hp(1500, 2)) * env(s, 0.0003, 0.004) + thump(140, 60, 0.05, s), 2.0)
    whoosh = tv_filter(noise(rng, s), lambda tt: bp(900 + 3600 * min(tt / 0.35, 1.0), 1.2)) * env(s, 0.01, 0.35, hold=0.05)
    return Track(1.8).add(reverb(rng, 0.8 * pop + 0.7 * whoosh, 0.8, 0.3, 3000)).buf


def rocket_fly(rng):
    s = 2.0
    body = noise(rng, s, 0.6, hp(120, 2), lp(4500, 2)) * (1 + 0.3 * wobble(rng, s, 30))
    hiss = noise(rng, s, 0.0, hp(5000, 2))
    rumble = noise(rng, s, 2.0, lp(200, 2))
    return body + 0.15 * hiss + 0.5 * rumble


# --- tones -------------------------------------------------------------------------------------------------------

def radar_alert(rng):
    out = Track(0.75)
    at = 0.0
    for freq, gap in ((988, 0.03), (740, 0.06), (988, 0.03), (740, 0.0)):
        out.add(tone(freq, 0.15, 'square', 0.005, 0.015, 0.6), at)
        at += 0.15 + gap
    return Track(1.1).add(reverb(rng, out.buf, 0.4, 0.15, 4000)).buf


def lock_seeking(rng):
    return tone(1050, 0.07, 'sine', 0.003, 0.02)


def lock_locked(rng):
    # Played every 2 ticks: 115 ms with soft edges, so the plays overlap into a steady tone.
    return tone(1650, 0.115, 'sine', 0.006, 0.006)


def warning_seeking(rng):
    out = Track(0.23)
    out.add(tone(560, 0.09, 'tri', 0.004, 0.02), 0.0)
    out.add(tone(560, 0.09, 'tri', 0.004, 0.02), 0.14)
    return out.buf


def warning_locked(rng):
    return tone(900, 0.14, 'square', 0.004, 0.02, 0.7)


def warning_incoming(rng):
    return tone(1350, 0.075, 'square', 0.003, 0.012, 0.7)


def designator_mark(rng):
    out = Track(0.2)
    out.add(sine(glide(3500, 1500, 0.02, 0.06), 0.06) * env(0.06, 0.001, 0.02), 0.0, 0.15)
    out.add(tone(1800, 0.045, 'tri', 0.003, 0.012), 0.0, 0.8)
    out.add(tone(2400, 0.06, 'tri', 0.003, 0.02), 0.07, 0.8)
    return out.buf


# --- blocks ------------------------------------------------------------------------------------------------------

def supply_crate_land(rng):
    s = 1.3
    out = Track(s)

    def impact(at, gain):
        out.add(thump(85, 40, 0.12, 0.6, tau=0.05), at, 1.0 * gain)
        out.add(noise(rng, 0.5, 2.0, lp(300, 2)) * env(0.5, 0.001, 0.1), at, 0.8 * gain)
        out.add(noise(rng, 0.3, 0.0, bp(900, 0.7)) * env(0.3, 0.0005, 0.045), at, 0.7 * gain)
        for _ in range(4):
            out.add(click(rng, rng.uniform(600, 2000), 0.006, 1.5), at + rng.uniform(0, 0.06), 0.4 * gain)

    impact(0.0, 1.0)
    out.add(metal(rng, (410, 1090, 2210, 3500), (0.35, 0.2, 0.12, 0.08), 0.8), 0.01, 0.35)
    impact(0.2, 0.35)
    out.add(noise(rng, 1.0, 0.0, lp(1200, 2)) * env(1.0, 0.02, 0.3), 0.05, 0.1)
    return Track(1.9).add(reverb(rng, out.buf, 0.6, 0.25, 3000)).buf


def silo_form_layer(rng):
    s = 1.0
    out = Track(s)
    out.add(thump(110, 55, 0.12, 0.6, tau=0.05), 0.0, 0.9)
    out.add(metal(rng, (180, 505, 1120, 2050), (0.3, 0.2, 0.12, 0.08), 0.8), 0.0, 0.5)
    out.add(click(rng, 2000, 0.004), 0.0, 0.6)
    hum = tv_filter(wave(110, 0.7) + 0.5 * sine(220, 0.7), lambda tt: lp(300 + 1200 * min(tt / 0.3, 1.0), 2))
    out.add(hum * env(0.7, 0.25, 0.25, hold=0.1), 0.08, 0.35)
    return Track(1.6).add(reverb(rng, out.buf, 0.9, 0.3, 3000)).buf


def silo_formed(rng):
    s = 2.8
    t = time(s)
    chord = np.zeros(n(s))
    for f in (110, 165, 220, 277.2, 330, 440):
        chord += sine(f * (1 + 0.02 * t), s, rng.uniform(0, 2 * np.pi))
    chord = tv_filter(chord, lambda tt: lp(400 + 5600 * min(tt / 1.8, 1.0) ** 2, 2))
    chord *= np.where(t < 1.8, (t / 1.8) ** 2, np.exp(-(t - 1.8) / 0.6))
    out = Track(s).add(chord, 0.0, 0.5)
    chime = Track(1.0)
    for f in (880, 1320, 1760):
        chime.add(sine(f, 1.0) * env(1.0, 0.001, 0.5) + 0.3 * sine(f * 2.76, 1.0) * env(1.0, 0.001, 0.15))
    out.add(chime.buf, 1.8, 0.3)
    out.add(thump(60, 35, 0.4, 1.0), 1.8, 0.8)
    return Track(4.4).add(reverb(rng, out.buf, 2.0, 0.4, 3500)).buf


def silo_unformed(rng):
    s = 1.8
    t = time(s)
    out = Track(s)
    out.add(thump(110, 55, 0.12, 0.6, tau=0.05), 0.0, 0.8)
    out.add(metal(rng, (180, 505, 1120), (0.3, 0.2, 0.12), 0.8), 0.0, 0.4)
    down = tv_filter(wave(55 + 385 * np.exp(-t / 0.35), s), lambda tt: lp(max(200.0, 3000.0 * np.exp(-tt / 0.4)), 2))
    out.add(down * env(s, 0.01, 0.6), 0.0, 0.5)
    out.add(sine(60, s) * env(s, 0.01, 0.8), 0.0, 0.25)
    return Track(2.6).add(reverb(rng, out.buf, 1.0, 0.3, 3000)).buf


# --- machine loops (seamless) ------------------------------------------------------------------------------------

def crusher_loop(rng):
    s = 3.0
    out = Track(s, loop=True)
    out.add((0.5 * periodic(50, s) + 0.3 * periodic(100, s)) * (1 + 0.1 * periodic(2, s)), 0.0, 0.6)
    out.add(filt(np.sign(periodic(150, s)) * 1.0, lp(400, 2)), 0.0, 0.1)
    out.add(noise(rng, s, 0.0, bp(700, 0.6)) * (0.6 + 0.4 * wobble(rng, s, 8)), 0.0, 0.3)
    for _ in range(14):
        at = rng.uniform(0, s)
        g = rng.uniform(0.4, 1.0)
        out.add(noise(rng, 0.12, 0.0, bp(rng.uniform(400, 1600), 1.0)) * env(0.12, 0.001, 0.03), at, 0.8 * g)
        out.add(thump(90, 70, 0.04, 0.2), at, 0.4 * g)
    return out.buf


def mixer_loop(rng):
    s = 3.0
    out = Track(s, loop=True)
    swirl = 1 + 0.15 * periodic(1, s)
    out.add((0.3 * periodic(120, s) + 0.2 * filt(np.sign(periodic(120, s)) * 1.0, lp(600, 2))) * swirl, 0.0, 0.7)
    out.add(noise(rng, s, 1.5, bp(350, 0.7)) * (0.5 + 0.25 * (periodic(1, s) + 1)), 0.0, 0.5)
    for _ in range(40):
        dur = rng.uniform(0.04, 0.08)
        f0 = rng.uniform(350, 1100)
        bubble = sine(f0 * (1 + 0.8 * time(dur) / dur), dur) * env(dur, 0.002, 0.02)
        out.add(bubble, rng.uniform(0, s), rng.uniform(0.2, 0.5))
    return out.buf


def fabricator_loop(rng):
    """A soft electrical hum with a faint, dark cooling fan under it, swelling very slowly. No beeps or other events."""
    s = 4.0
    out = Track(s, loop=True)
    hum = filt(0.5 * periodic(100, s) + 0.25 * periodic(200, s) + 0.1 * periodic(300, s), lp(400, 2))
    out.add(hum * (1 + 0.08 * periodic(0.25, s)), 0.0, 0.5)
    out.add(noise(rng, s, 1.0, lp(1000, 2)) * (1 + 0.1 * periodic(0.5, s)), 0.0, 0.06)
    return out.buf


def assembler_loop(rng):
    """A robot arm at work: a soft, low servo whir that glides up while the arm moves and back down on the return,
    with a gentle gear purr and a muffled stop at the end of each move, over a quiet hum. Only sines and dark
    noise, so nothing in it is sharp."""
    s = 4.0
    t = time(s)
    out = Track(s, loop=True)
    out.add(filt(periodic(60, s), lp(200, 2)), 0.0, 0.15)
    # Two moves per loop: up from 0.2 s to 1.4 s, back down from 2.2 s to 3.4 s.
    level = np.zeros(n(s))
    freq = np.full(n(s), 140.0)
    for start, end, f0, f1 in ((0.2, 1.4, 140.0, 190.0), (2.2, 3.4, 190.0, 140.0)):
        inside = (t >= start) & (t < end)
        x = (t[inside] - start) / (end - start)
        level[inside] = np.sin(np.pi * x) ** 2
        freq[inside] = f0 + (f1 - f0) * (0.5 - 0.5 * np.cos(np.pi * x))
        freq[t >= end] = f1
        out.add(filt(thump(70, 50, 0.05, 0.3), lp(400, 2)), end - 0.05, 0.25)
    whir = sine(freq, s) + 0.3 * sine(2 * freq, s) + 0.1 * sine(3 * freq, s)
    purr = noise(rng, s, 1.0, bp(500, 1.0)) * (0.6 + 0.4 * periodic(25, s))
    out.add((whir + 0.5 * purr) * level, 0.0, 0.5)
    return out.buf


def generator_loop(rng):
    """The burner's low rumble and its soft roar, without beats or crackle."""
    s = 2.0
    out = Track(s, loop=True)
    out.add(noise(rng, s, 2.0, lp(150, 2)), 0.0, 0.4)
    out.add(noise(rng, s, 0.0, bp(800, 0.5)), 0.0, 0.08)
    return out.buf


def charging_station_loop(rng):
    s = 2.0
    out = Track(s, loop=True)
    out.add(0.3 * periodic(120, s) + 0.15 * periodic(240, s) + 0.07 * periodic(360, s))
    out.add(periodic(2000, s) * (0.5 + 0.5 * periodic(0.5, s)), 0.0, 0.06)
    for _ in range(4):
        out.add(noise(rng, 0.05, 0.0, hp(4000, 2)) * env(0.05, 0.0005, 0.003), rng.uniform(0, s), 0.15)
    return out.buf


# --- output ------------------------------------------------------------------------------------------------------

def finish(x, peak_db=-1.0, rms_db=None, loop=False):
    x = x - np.mean(x)
    if not loop:
        # Fade the end out, so a tail cut off by the sound's length doesn't stop abruptly.
        fade = min(len(x) // 5, n(0.15))
        x[-fade:] *= 0.5 + 0.5 * np.cos(np.linspace(0.0, np.pi, fade))
    if rms_db is not None:
        x = x * (10 ** (rms_db / 20) / (np.sqrt(np.mean(x ** 2)) + 1e-12))
        limit = 10 ** (peak_db / 20)
        if np.max(np.abs(x)) > limit:
            x = x * (limit / np.max(np.abs(x)))
        return x
    return x * (10 ** (peak_db / 20) / (np.max(np.abs(x)) + 1e-12))


def write_ogg(path, x):
    path.parent.mkdir(parents=True, exist_ok=True)
    pcm = (np.clip(x, -1.0, 1.0) * 32767).astype('<i2').tobytes()
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-f', 's16le', '-ar', str(SR), '-ac', '1', '-i', '-',
                    '-c:a', 'libvorbis', '-q:a', '5', '-map_metadata', '-1', '-fflags', '+bitexact', '-flags:a', '+bitexact',
                    str(path)], input=pcm, check=True)


def gun(name):
    return lambda rng: gunshot(rng, GUNS[name])


# event: (file, generator, variants, peak dBFS, extra sounds.json fields, loop). Loops are normalized by loudness
# (RMS) instead of peak, since they play continuously.
EVENTS = {
    'weapon.pistol.fire': ('weapon/pistol_fire', gun('pistol'), 3, -1.0, {'attenuation_distance': 48}),
    'weapon.auto_pistol.fire': ('weapon/auto_pistol_fire', gun('auto_pistol'), 3, -1.5, {'attenuation_distance': 48}),
    'weapon.smg.fire': ('weapon/smg_fire', gun('smg'), 3, -2.0, {'attenuation_distance': 48}),
    'weapon.shotgun.fire': ('weapon/shotgun_fire', gun('shotgun'), 3, -0.5, {'attenuation_distance': 48}),
    'weapon.sniper_rifle.fire': ('weapon/sniper_rifle_fire', gun('sniper_rifle'), 3, -0.5, {'attenuation_distance': 96}),
    'weapon.gatling_gun.fire': ('weapon/gatling_gun_fire', gun('gatling_gun'), 3, -4.0, {'attenuation_distance': 48}),
    'weapon.gatling_gun.spin': ('weapon/gatling_gun_spin', gatling_spin, 1, -4.0, {'attenuation_distance': 32}),
    'weapon.rocket_launcher.fire': ('weapon/rocket_launcher_fire', rocket_launcher_fire, 2, -0.5, {'attenuation_distance': 64}),
    'weapon.rocket.explode': ('weapon/rocket_explode', rocket_explode, 2, -0.5, {}),
    'weapon.empty': ('weapon/empty', weapon_empty, 1, -8.0, {}),
    'weapon.reload': ('weapon/reload', weapon_reload, 3, -7.0, {}),
    'weapon.reload_done': ('weapon/reload_done', weapon_reload_done, 1, -6.0, {}),
    'weapon.hit': ('weapon/hit', hit_marker, 2, -5.0, {}),
    'weapon.headshot': ('weapon/headshot', headshot, 1, -4.0, {}),
    'rocket.fly': ('weapon/rocket_fly', rocket_fly, 1, -2.0, {'attenuation_distance': 32}, True),
    'lock.seeking': ('ui/lock_seeking', lock_seeking, 1, -6.0, {}),
    'lock.locked': ('ui/lock_locked', lock_locked, 1, -7.0, {}),
    'warning.seeking': ('ui/warning_seeking', warning_seeking, 1, -6.0, {}),
    'warning.locked': ('ui/warning_locked', warning_locked, 1, -7.0, {}),
    'warning.incoming': ('ui/warning_incoming', warning_incoming, 1, -7.0, {}),
    'missile.launch': ('missile/launch', missile_launch, 1, -0.5, {}),
    'missile.incoming': ('missile/incoming', missile_incoming, 1, -0.5, {}),
    'missile.explode': ('missile/explode', missile_explode, 3, -0.3, {}),
    'missile.emp': ('missile/emp', missile_emp, 1, -0.5, {}),
    'missile.shot_down': ('missile/shot_down', missile_shot_down, 2, -0.5, {}),
    'designator.mark': ('missile/designator_mark', designator_mark, 1, -8.0, {}),
    'radar.alert': ('missile/radar_alert', radar_alert, 1, -5.0, {}),
    'interceptor.launch': ('missile/interceptor_launch', interceptor_launch, 2, -1.0, {}),
    'silo.form_layer': ('missile/silo_form_layer', silo_form_layer, 1, -3.0, {}),
    'silo.formed': ('missile/silo_formed', silo_formed, 1, -2.0, {}),
    'silo.unformed': ('missile/silo_unformed', silo_unformed, 1, -3.0, {}),
    'supply_crate.land': ('block/supply_crate_land', supply_crate_land, 2, -1.0, {'attenuation_distance': 32}),
    'machine.crusher': ('block/crusher', crusher_loop, 1, -24.0, {'attenuation_distance': 12}, True),
    'machine.chemical_mixer': ('block/chemical_mixer', mixer_loop, 1, -25.0, {'attenuation_distance': 12}, True),
    'machine.circuit_fabricator': ('block/circuit_fabricator', fabricator_loop, 1, -27.0, {'attenuation_distance': 12}, True),
    'machine.assembler': ('block/assembler', assembler_loop, 1, -25.0, {'attenuation_distance': 12}, True),
    'combustion_generator.running': ('block/combustion_generator', generator_loop, 1, -23.0, {'attenuation_distance': 12}, True),
    'charging_station.running': ('block/charging_station', charging_station_loop, 1, -30.0, {'attenuation_distance': 8}, True),
}


def main():
    definitions = {}
    for event, spec in sorted(EVENTS.items()):
        file, make, variants, level, extra = spec[:5]
        loop = len(spec) > 5 and spec[5]
        sounds = []
        for i in range(variants):
            rng = np.random.default_rng(zlib.crc32(f'{event}/{i}'.encode()))
            x = make(rng)
            x = finish(x, rms_db=level, loop=True) if loop else finish(x, peak_db=level)
            name = file if variants == 1 else f'{file}_{i + 1}'
            write_ogg(SOUNDS / f'{name}.ogg', x)
            sounds.append({'name': f'fusionfactions:{name}', **extra})
        definitions[event] = {'subtitle': f'subtitles.fusionfactions.{event}', 'sounds': sounds}
    with open(ROOT / 'sounds.json', 'w') as out:
        json.dump(definitions, out, indent=2)
        out.write('\n')
    print(f'wrote {sum(len(d["sounds"]) for d in definitions.values())} sounds for {len(definitions)} events')


if __name__ == '__main__':
    main()
