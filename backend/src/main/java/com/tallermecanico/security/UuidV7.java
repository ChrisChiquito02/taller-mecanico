package com.tallermecanico.security;

import java.security.SecureRandom;
import java.util.UUID;

/** Genera UUIDv7 con los 48 bits iniciales basados en milisegundos Unix (fecha). */
public final class UuidV7 {
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Impide crear instancias de la utilidad de UUIDv7. */
    private UuidV7() {}

    /** Genera un UUID versión 7 cuya marca temporal permite ordenar los identificadores por fecha. */
    public static UUID generate() {
        long millis = System.currentTimeMillis();
        byte[] bytes = new byte[16]; RANDOM.nextBytes(bytes);
        long msb = ((millis & 0xFFFFFFFFFFFFL) << 16) | 0x7000L | (RANDOM.nextInt(1 << 12));
        long lsb = (((long) RANDOM.nextInt(1 << 14)) | 0x8000L) << 48;
        for (int i = 0; i < 6; i++) lsb |= ((long) bytes[i] & 0xff) << (40 - 8 * i);
        return new UUID(msb, lsb);
    }
}
