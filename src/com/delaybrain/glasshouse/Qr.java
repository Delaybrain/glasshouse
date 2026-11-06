package com.delaybrain.glasshouse;

import java.nio.charset.StandardCharsets;

// A QR code for a short text (the photo page's address), so a phone's camera can open it. Just what that
// needs: byte mode, error correction M, versions 1-3 (up to 42 bytes), one block, the mask with the
// lowest penalty. The steps follow the QR standard (ISO/IEC 18004) as Project Nayuki's generator lays
// them out. No library: this app has none.
final class Qr {
    private static final int[] DATA = {0, 16, 28, 44}; // data codewords per version, level M
    private static final int[] ECC = {0, 10, 16, 26};  // error correction codewords per version, level M

    private final int size;
    private final boolean[][] dark, fixed; // [y][x]

    private Qr(int version) {
        size = 17 + 4 * version;
        dark = new boolean[size][size];
        fixed = new boolean[size][size];
    }

    // The modules, [y][x], true for dark; null when the text is too long for version 3.
    static boolean[][] encode(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        int version = 1;
        while (version <= 3 && 4 + 8 + 8 * bytes.length > DATA[version] * 8) version++;
        if (version > 3) return null;

        // Mode (byte), length, the bytes, a terminator, then padding to the codewords there's room for.
        int cap = DATA[version] * 8;
        Bits bits = new Bits(cap);
        bits.add(0x4, 4);
        bits.add(bytes.length, 8);
        for (byte b : bytes) bits.add(b & 0xFF, 8);
        bits.add(0, Math.min(4, cap - bits.n));
        bits.add(0, (8 - bits.n % 8) % 8);
        for (int pad = 0xEC; bits.n < cap; pad ^= 0xEC ^ 0x11) bits.add(pad, 8);
        byte[] data = bits.bytes();
        byte[] ecc = remainder(data, divisor(ECC[version]));
        byte[] all = new byte[data.length + ecc.length];
        System.arraycopy(data, 0, all, 0, data.length);
        System.arraycopy(ecc, 0, all, data.length, ecc.length);

        Qr q = new Qr(version);
        q.drawFixed(version);
        q.drawCodewords(all);
        int best = 0, lowest = Integer.MAX_VALUE;
        for (int m = 0; m < 8; m++) {
            q.mask(m);
            q.drawFormat(m);
            int p = q.penalty();
            if (p < lowest) { lowest = p; best = m; }
            q.mask(m); // XOR again: undone
        }
        q.mask(best);
        q.drawFormat(best);
        return q.dark;
    }

    private void set(int x, int y, boolean on) {
        dark[y][x] = on;
        fixed[y][x] = true;
    }

    // Finders in three corners, timing lines, the alignment square (version 2 and 3), room for format bits.
    private void drawFixed(int version) {
        for (int i = 0; i < size; i++) {
            set(6, i, i % 2 == 0);
            set(i, 6, i % 2 == 0);
        }
        finder(3, 3);
        finder(size - 4, 3);
        finder(3, size - 4);
        if (version >= 2) {
            int at = 4 * version + 10;
            for (int dy = -2; dy <= 2; dy++)
                for (int dx = -2; dx <= 2; dx++) set(at + dx, at + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
        }
        drawFormat(0); // marks the format area as fixed; drawn for real once the mask is chosen
    }

    private void finder(int x, int y) {
        for (int dy = -4; dy <= 4; dy++)
            for (int dx = -4; dx <= 4; dx++) {
                int d = Math.max(Math.abs(dx), Math.abs(dy)), xx = x + dx, yy = y + dy;
                if (xx >= 0 && xx < size && yy >= 0 && yy < size) set(xx, yy, d != 2 && d != 4);
            }
    }

    // Level M (format bits 00) and the mask, with their BCH check bits, twice, and the dark module.
    private void drawFormat(int mask) {
        int data = mask; // M is 00 << 3
        int rem = data;
        for (int i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        int f = (data << 10 | rem) ^ 0x5412;
        for (int i = 0; i <= 5; i++) set(8, i, bit(f, i));
        set(8, 7, bit(f, 6));
        set(8, 8, bit(f, 7));
        set(7, 8, bit(f, 8));
        for (int i = 9; i < 15; i++) set(14 - i, 8, bit(f, i));
        for (int i = 0; i < 8; i++) set(size - 1 - i, 8, bit(f, i));
        for (int i = 8; i < 15; i++) set(8, size - 15 + i, bit(f, i));
        set(8, size - 8, true);
    }

    // The codewords in the zigzag: two columns at a time from the right, up then down, skipping column 6.
    private void drawCodewords(byte[] cw) {
        int i = 0;
        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) right = 5;
            for (int vert = 0; vert < size; vert++)
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    boolean up = ((right + 1) & 2) == 0;
                    int y = up ? size - 1 - vert : vert;
                    if (!fixed[y][x] && i < cw.length * 8) {
                        dark[y][x] = bit(cw[i >>> 3], 7 - (i & 7));
                        i++;
                    }
                }
        }
    }

    private void mask(int m) {
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                if (fixed[y][x]) continue;
                boolean flip;
                switch (m) {
                    case 0: flip = (x + y) % 2 == 0; break;
                    case 1: flip = y % 2 == 0; break;
                    case 2: flip = x % 3 == 0; break;
                    case 3: flip = (x + y) % 3 == 0; break;
                    case 4: flip = (x / 3 + y / 2) % 2 == 0; break;
                    case 5: flip = x * y % 2 + x * y % 3 == 0; break;
                    case 6: flip = (x * y % 2 + x * y % 3) % 2 == 0; break;
                    default: flip = ((x + y) % 2 + x * y % 3) % 2 == 0;
                }
                if (flip) dark[y][x] = !dark[y][x];
            }
    }

    // How hard the code is to read: long runs of one colour, 2x2 blocks, and too much of one colour.
    private int penalty() {
        int p = 0, count = 0;
        for (int y = 0; y < size; y++) {
            int runX = 1, runY = 1;
            for (int x = 0; x < size; x++) {
                if (dark[y][x]) count++;
                if (x > 0) {
                    if (dark[y][x] == dark[y][x - 1]) { if (++runX == 5) p += 3; else if (runX > 5) p++; } else runX = 1;
                    if (dark[x][y] == dark[x - 1][y]) { if (++runY == 5) p += 3; else if (runY > 5) p++; } else runY = 1;
                }
                if (x > 0 && y > 0 && dark[y][x] == dark[y][x - 1] && dark[y][x] == dark[y - 1][x] && dark[y][x] == dark[y - 1][x - 1])
                    p += 3;
            }
        }
        int total = size * size;
        p += (Math.abs(count * 20 - total * 10) + total - 1) / total * 10;
        return p;
    }

    // Reed-Solomon over GF(2^8) with the QR polynomial 0x11D.
    private static byte[] divisor(int degree) {
        byte[] r = new byte[degree];
        r[degree - 1] = 1;
        int root = 1;
        for (int i = 0; i < degree; i++) {
            for (int j = 0; j < r.length; j++) {
                r[j] = (byte) mul(r[j] & 0xFF, root);
                if (j + 1 < r.length) r[j] ^= r[j + 1];
            }
            root = mul(root, 0x02);
        }
        return r;
    }

    private static byte[] remainder(byte[] data, byte[] div) {
        byte[] r = new byte[div.length];
        for (byte b : data) {
            int factor = (b ^ r[0]) & 0xFF;
            System.arraycopy(r, 1, r, 0, r.length - 1);
            r[r.length - 1] = 0;
            for (int i = 0; i < r.length; i++) r[i] ^= (byte) mul(div[i] & 0xFF, factor);
        }
        return r;
    }

    private static int mul(int x, int y) {
        int z = 0;
        for (int i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * 0x11D);
            z ^= ((y >>> i) & 1) * x;
        }
        return z;
    }

    private static boolean bit(int v, int i) {
        return ((v >>> i) & 1) != 0;
    }

    private static final class Bits {
        final boolean[] b;
        int n;
        Bits(int cap) { b = new boolean[cap]; }
        void add(int v, int len) { for (int i = len - 1; i >= 0; i--) b[n++] = ((v >>> i) & 1) != 0; }
        byte[] bytes() {
            byte[] out = new byte[n / 8];
            for (int i = 0; i < n; i++) if (b[i]) out[i >>> 3] |= (byte) (0x80 >>> (i & 7));
            return out;
        }
    }
}
