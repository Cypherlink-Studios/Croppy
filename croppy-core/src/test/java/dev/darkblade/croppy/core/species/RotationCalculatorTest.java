package dev.darkblade.croppy.core.species;

import dev.darkblade.croppy.api.species.RotationMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotationCalculatorTest {

    @Test
    void testNormalize() {
        assertEquals(0.0f, RotationCalculator.normalize(0.0f), 0.001f);
        assertEquals(90.0f, RotationCalculator.normalize(90.0f), 0.001f);
        assertEquals(0.0f, RotationCalculator.normalize(360.0f), 0.001f);
        assertEquals(270.0f, RotationCalculator.normalize(-90.0f), 0.001f);
        assertEquals(180.0f, RotationCalculator.normalize(540.0f), 0.001f);
    }

    @Test
    void testSnapCardinal4() {
        assertEquals(0.0f, RotationCalculator.snapCardinal4(12.0f), 0.001f);
        assertEquals(90.0f, RotationCalculator.snapCardinal4(85.0f), 0.001f);
        assertEquals(180.0f, RotationCalculator.snapCardinal4(190.0f), 0.001f);
        assertEquals(270.0f, RotationCalculator.snapCardinal4(260.0f), 0.001f);
        assertEquals(0.0f, RotationCalculator.snapCardinal4(355.0f), 0.001f);
    }

    @Test
    void testSnapCardinal8() {
        assertEquals(0.0f, RotationCalculator.snapCardinal8(10.0f), 0.001f);
        assertEquals(45.0f, RotationCalculator.snapCardinal8(40.0f), 0.001f);
        assertEquals(90.0f, RotationCalculator.snapCardinal8(92.0f), 0.001f);
        assertEquals(135.0f, RotationCalculator.snapCardinal8(130.0f), 0.001f);
        assertEquals(315.0f, RotationCalculator.snapCardinal8(320.0f), 0.001f);
    }

    @Test
    void testCalculateModes() {
        assertEquals(90.0f, RotationCalculator.calculate(80.0f, RotationMode.CARDINAL_4), 0.001f);
        assertEquals(45.0f, RotationCalculator.calculate(40.0f, RotationMode.CARDINAL_8), 0.001f);
        assertEquals(37.5f, RotationCalculator.calculate(37.5f, RotationMode.FREE_YAW), 0.001f);

        float jittered = RotationCalculator.calculate(100.0f, RotationMode.RANDOM_JITTER);
        assertTrue(jittered >= 85.0f && jittered <= 115.0f);

        assertEquals(105.0f, RotationCalculator.calculate(100.0f, RotationMode.RANDOM_JITTER, 5.0f), 0.001f);
    }

    @Test
    void testToYQuaternion() {
        float[] q0 = RotationCalculator.toYQuaternion(0.0f);
        assertArrayEquals(new float[]{0.0f, 0.0f, 0.0f, 1.0f}, q0, 0.001f);

        float[] q180 = RotationCalculator.toYQuaternion(180.0f);
        assertEquals(0.0f, q180[0], 0.001f);
        assertEquals(-1.0f, q180[1], 0.001f);
        assertEquals(0.0f, q180[2], 0.001f);
        assertEquals(0.0f, q180[3], 0.001f);
    }
}
