package fr.irit.complex.utils;

import fr.irit.input.ParameterException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class UtilsTest {

    @Test
    void init() throws IOException, ParameterException {
        Utils utils = Utils.getInstance();
        utils.init("runConfig.json");
    }
}