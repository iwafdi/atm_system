package util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PinHasherTest {

    @Test
    void correctPinVerifies() {
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash("1234", salt);
        assertTrue(PinHasher.verify("1234", salt, hash));
    }

    @Test
    void wrongPinRejected() {
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash("1234", salt);
        assertFalse(PinHasher.verify("0000", salt, hash));
    }

    @Test
    void samePinDifferentSaltsProduceDifferentHashes() {
        String hashA = PinHasher.hash("1234", PinHasher.newSalt());
        String hashB = PinHasher.hash("1234", PinHasher.newSalt());
        assertNotEquals(hashA, hashB);
    }

    @Test
    void hashIsDeterministicForSameSalt() {
        String salt = PinHasher.newSalt();
        assertEquals(PinHasher.hash("9999", salt), PinHasher.hash("9999", salt));
    }
}
