package com.vehiclerental;

import com.vehiclerental.model.VehicleStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VehicleStatusTest {
    @Test
    void shouldParseStatusValuesFromDatabase() {
        assertEquals(VehicleStatus.AVAILABLE, VehicleStatus.fromDbValue("AVAILABLE"));
        assertEquals(VehicleStatus.BOOKED, VehicleStatus.fromDbValue("BOOKED"));
        assertEquals(VehicleStatus.AVAILABLE, VehicleStatus.fromDbValue("unknown"));
    }
}
