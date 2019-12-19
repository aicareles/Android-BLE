package cn.com.heaton.blelibrary.ble.utils;

public class CrcUtils {
    public static class CRC8 {
        public static int CRC8(byte[] source, int offset, int length) {
            int wCRCin = 0x00;
            int wCPoly = 0x07;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                for (int j = 0; j < 8; j++) {
                    boolean bit = ((source[i] >> (7 - j) & 1) == 1);
                    boolean c07 = ((wCRCin >> 7 & 1) == 1);
                    wCRCin <<= 1;
                    if (c07 ^ bit)
                        wCRCin ^= wCPoly;
                }
            }
            wCRCin &= 0xFF;
            return wCRCin ^= 0x00;
        }

        public static int CRC8_DARC(byte[] source, int offset, int length) {
            int wCRCin = 0x00;
            // Integer.reverse(0x39) >>> 24
            int wCPoly = 0x9C;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                wCRCin ^= ((long) source[i] & 0xFF);
                for (int j = 0; j < 8; j++) {
                    if ((wCRCin & 0x01) != 0) {
                        wCRCin >>= 1;
                        wCRCin ^= wCPoly;
                    } else {
                        wCRCin >>= 1;
                    }
                }
            }
            return wCRCin ^= 0x00;
        }

        public static int CRC8_ITU(byte[] source, int offset, int length) {
            int wCRCin = 0x00;
            int wCPoly = 0x07;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                for (int j = 0; j < 8; j++) {
                    boolean bit = ((source[i] >> (7 - j) & 1) == 1);
                    boolean c07 = ((wCRCin >> 7 & 1) == 1);
                    wCRCin <<= 1;
                    if (c07 ^ bit)
                        wCRCin ^= wCPoly;
                }
            }
            wCRCin &= 0xFF;
            return wCRCin ^= 0x55;
        }

        public static int CRC8_MAXIM(byte[] source, int offset, int length) {
            int wCRCin = 0x00;
            // Integer.reverse(0x31) >>> 24
            int wCPoly = 0x8C;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                wCRCin ^= ((long) source[i] & 0xFF);
                for (int j = 0; j < 8; j++) {
                    if ((wCRCin & 0x01) != 0) {
                        wCRCin >>= 1;
                        wCRCin ^= wCPoly;
                    } else {
                        wCRCin >>= 1;
                    }
                }
            }
            return wCRCin ^= 0x00;
        }

        public static int CRC8_ROHC(byte[] source, int offset, int length) {
            int wCRCin = 0xFF;
            // Integer.reverse(0x07) >>> 24
            int wCPoly = 0xE0;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                wCRCin ^= ((long) source[i] & 0xFF);
                for (int j = 0; j < 8; j++) {
                    if ((wCRCin & 0x01) != 0) {
                        wCRCin >>= 1;
                        wCRCin ^= wCPoly;
                    } else {
                        wCRCin >>= 1;
                    }
                }
            }
            return wCRCin ^= 0x00;
        }

    }
    public static class CRC16 {
        public static int CRC16_IBM(byte[] source, int offset, int length) {
            int wCRCin = 0x0000;
            // Integer.reverse(0x8005) >>> 16
            int wCPoly = 0xA001;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                wCRCin ^= ((int) source[i] & 0x00FF);
                for (int j = 0; j < 8; j++) {
                    if ((wCRCin & 0x0001) != 0) {
                        wCRCin >>= 1;
                        wCRCin ^= wCPoly;
                    } else {
                        wCRCin >>= 1;
                    }
                }
            }
            return wCRCin ^= 0x0000;
        }

        public static int CRC16_CCITT(byte[] source, int offset, int length) {
            int wCRCin = 0x0000;
            // Integer.reverse(0x1021) >>> 16
            int wCPoly = 0x8408;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                wCRCin ^= ((int) source[i] & 0x00FF);
                for (int j = 0; j < 8; j++) {
                    if ((wCRCin & 0x0001) != 0) {
                        wCRCin >>= 1;
                        wCRCin ^= wCPoly;
                    } else {
                        wCRCin >>= 1;
                    }
                }
            }
            return wCRCin ^= 0x0000;
        }

        public static int CRC16_CCITT_FALSE(byte[] source, int offset, int length) {
            int wCRCin = 0xFFFF;
            int wCPoly = 0x1021;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                for (int j = 0; j < 8; j++) {
                    boolean bit = ((source[i] >> (7 - j) & 1) == 1);
                    boolean c15 = ((wCRCin >> 15 & 1) == 1);
                    wCRCin <<= 1;
                    if (c15 ^ bit)
                        wCRCin ^= wCPoly;
                }
            }
            wCRCin &= 0xFFFF;
            return wCRCin ^= 0x0000;
        }

        public static int CRC16_DECT_R(byte[] source, int offset, int length) {
            int wCRCin = 0x0000;
            int wCPoly = 0x0589;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                for (int j = 0; j < 8; j++) {
                    boolean bit = ((source[i] >> (7 - j) & 1) == 1);
                    boolean c15 = ((wCRCin >> 15 & 1) == 1);
                    wCRCin <<= 1;
                    if (c15 ^ bit)
                        wCRCin ^= wCPoly;
                }
            }
            wCRCin &= 0xFFFF;
            return wCRCin ^= 0x0001;
        }

        public static int CRC16_DECT_X(byte[] source, int offset, int length) {
            int wCRCin = 0x0000;
            int wCPoly = 0x0589;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                for (int j = 0; j < 8; j++) {
                    boolean bit = ((source[i] >> (7 - j) & 1) == 1);
                    boolean c15 = ((wCRCin >> 15 & 1) == 1);
                    wCRCin <<= 1;
                    if (c15 ^ bit)
                        wCRCin ^= wCPoly;
                }
            }
            wCRCin &= 0xFFFF;
            return wCRCin ^= 0x0000;
        }

        public static int CRC16_DNP(byte[] source, int offset, int length) {
            int wCRCin = 0x0000;
            // Integer.reverse(0x3D65) >>> 16
            int wCPoly = 0xA6BC;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                wCRCin ^= ((int) source[i] & 0x00FF);
                for (int j = 0; j < 8; j++) {
                    if ((wCRCin & 0x0001) != 0) {
                        wCRCin >>= 1;
                        wCRCin ^= wCPoly;
                    } else {
                        wCRCin >>= 1;
                    }
                }
            }
            return wCRCin ^= 0xFFFF;
        }

        public static int CRC16_GENIBUS(byte[] source, int offset, int length) {
            int wCRCin = 0xFFFF;
            int wCPoly = 0x1021;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                for (int j = 0; j < 8; j++) {
                    boolean bit = ((source[i] >> (7 - j) & 1) == 1);
                    boolean c15 = ((wCRCin >> 15 & 1) == 1);
                    wCRCin <<= 1;
                    if (c15 ^ bit)
                        wCRCin ^= wCPoly;
                }
            }
            wCRCin &= 0xFFFF;
            return wCRCin ^= 0xFFFF;
        }

        public static int CRC16_MAXIM(byte[] source, int offset, int length) {
            int wCRCin = 0x0000;
            // Integer.reverse(0x8005) >>> 16
            int wCPoly = 0xA001;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                wCRCin ^= ((int) source[i] & 0x00FF);
                for (int j = 0; j < 8; j++) {
                    if ((wCRCin & 0x0001) != 0) {
                        wCRCin >>= 1;
                        wCRCin ^= wCPoly;
                    } else {
                        wCRCin >>= 1;
                    }
                }
            }
            return wCRCin ^= 0xFFFF;
        }

        public static int CRC16_MODBUS(byte[] source, int offset, int length) {
            int wCRCin = 0xFFFF;
            // Integer.reverse(0x8005) >>> 16
            int wCPoly = 0xA001;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                wCRCin ^= ((int) source[i] & 0x00FF);
                for (int j = 0; j < 8; j++) {
                    if ((wCRCin & 0x0001) != 0) {
                        wCRCin >>= 1;
                        wCRCin ^= wCPoly;
                    } else {
                        wCRCin >>= 1;
                    }
                }
            }
            return wCRCin ^= 0x0000;
        }

        public static int CRC16_USB(byte[] source, int offset, int length) {
            int wCRCin = 0xFFFF;
            // Integer.reverse(0x8005) >>> 16
            int wCPoly = 0xA001;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                wCRCin ^= ((int) source[i] & 0x00FF);
                for (int j = 0; j < 8; j++) {
                    if ((wCRCin & 0x0001) != 0) {
                        wCRCin >>= 1;
                        wCRCin ^= wCPoly;
                    } else {
                        wCRCin >>= 1;
                    }
                }
            }
            return wCRCin ^= 0xFFFF;
        }

        public static int CRC16_X25(byte[] source, int offset, int length) {
            int wCRCin = 0xFFFF;
            // Integer.reverse(0x1021) >>> 16
            int wCPoly = 0x8408;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                wCRCin ^= ((int) source[i] & 0x00FF);
                for (int j = 0; j < 8; j++) {
                    if ((wCRCin & 0x0001) != 0) {
                        wCRCin >>= 1;
                        wCRCin ^= wCPoly;
                    } else {
                        wCRCin >>= 1;
                    }
                }
            }
            return wCRCin ^= 0xFFFF;
        }

        public static int CRC16_XMODEM(byte[] source, int offset, int length) {
            int wCRCin = 0x0000;
            int wCPoly = 0x1021;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                for (int j = 0; j < 8; j++) {
                    boolean bit = ((source[i] >> (7 - j) & 1) == 1);
                    boolean c15 = ((wCRCin >> 15 & 1) == 1);
                    wCRCin <<= 1;
                    if (c15 ^ bit)
                        wCRCin ^= wCPoly;
                }
            }
            wCRCin &= 0xFFFF;
            return wCRCin ^= 0x0000;
        }

    }

    public static class CRC32 {
        public static long CRC32(byte[] source, int offset, int length) {
            long wCRCin = 0xFFFFFFFFL;
            // Long.reverse(0x04C11DB7L) >>> 32
            long wCPoly = 0xEDB88320L;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                wCRCin ^= ((long) source[i] & 0x000000FFL);
                for (int j = 0; j < 8; j++) {
                    if ((wCRCin & 0x00000001L) != 0) {
                        wCRCin >>= 1;
                        wCRCin ^= wCPoly;
                    } else {
                        wCRCin >>= 1;
                    }
                }
            }
            return wCRCin ^= 0xFFFFFFFFL;
        }

        public static long CRC32_B(byte[] source, int offset, int length) {
            long wCRCin = 0xFFFFFFFFL;
            long wCPoly = 0x04C11DB7L;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                for (int j = 0; j < 8; j++) {
                    boolean bit = ((source[i] >> (7 - j) & 1) == 1);
                    boolean c31 = ((wCRCin >> 31 & 1) == 1);
                    wCRCin <<= 1;
                    if (c31 ^ bit) {
                        wCRCin ^= wCPoly;
                    }
                }
            }
            wCRCin &= 0xFFFFFFFFL;
            return wCRCin ^= 0xFFFFFFFFL;
        }

        public static long CRC32_C(byte[] source, int offset, int length) {
            long wCRCin = 0xFFFFFFFFL;
            // Long.reverse(0x1EDC6F41L) >>> 32
            long wCPoly = 0x82F63B78L;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                wCRCin ^= ((long) source[i] & 0x000000FFL);
                for (int j = 0; j < 8; j++) {
                    if ((wCRCin & 0x00000001L) != 0) {
                        wCRCin >>= 1;
                        wCRCin ^= wCPoly;
                    } else {
                        wCRCin >>= 1;
                    }
                }
            }
            return wCRCin ^= 0xFFFFFFFFL;
        }

        public static long CRC32_D(byte[] source, int offset, int length) {
            long wCRCin = 0xFFFFFFFFL;
            // Long.reverse(0xA833982BL) >>> 32
            long wCPoly = 0xD419CC15L;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                wCRCin ^= ((long) source[i] & 0x000000FFL);
                for (int j = 0; j < 8; j++) {
                    if ((wCRCin & 0x00000001L) != 0) {
                        wCRCin >>= 1;
                        wCRCin ^= wCPoly;
                    } else {
                        wCRCin >>= 1;
                    }
                }
            }
            return wCRCin ^= 0xFFFFFFFFL;
        }

        public static long CRC32_MPEG_2(byte[] source, int offset, int length) {
            long wCRCin = 0xFFFFFFFFL;
            long wCPoly = 0x04C11DB7L;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                for (int j = 0; j < 8; j++) {
                    boolean bit = ((source[i] >> (7 - j) & 1) == 1);
                    boolean c31 = ((wCRCin >> 31 & 1) == 1);
                    wCRCin <<= 1;
                    if (c31 ^ bit) {
                        wCRCin ^= wCPoly;
                    }
                }
            }
            wCRCin &= 0xFFFFFFFFL;
            return wCRCin ^= 0x00000000L;
        }

        public static long CRC32_POSIX(byte[] source, int offset, int length) {
            long wCRCin = 0x00000000L;
            long wCPoly = 0x04C11DB7L;
            for (int i = offset, cnt = offset + length; i < cnt; i++) {
                for (int j = 0; j < 8; j++) {
                    boolean bit = ((source[i] >> (7 - j) & 1) == 1);
                    boolean c31 = ((wCRCin >> 31 & 1) == 1);
                    wCRCin <<= 1;
                    if (c31 ^ bit) {
                        wCRCin ^= wCPoly;
                    }
                }
            }
            wCRCin &= 0xFFFFFFFFL;
            return wCRCin ^= 0xFFFFFFFFL;
        }
    }
}
