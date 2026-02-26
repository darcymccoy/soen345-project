package com.example.soen345;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MainActivityTest {

    private MainActivity activity;

    @BeforeEach
    void setUp() {
        activity = new MainActivity();
    }

    @Test
    void addReturnsSumOfTwoNumbers() {
        assertEquals(5, activity.add(2, 3));
    }

}
