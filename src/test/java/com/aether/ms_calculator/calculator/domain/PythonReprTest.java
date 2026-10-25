package com.aether.ms_calculator.calculator.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

class PythonReprTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    @Test
    void formatsNullAndBooleansLikePython() {
        assertEquals("None", PythonRepr.of(NODES.nullNode()));
        assertEquals("True", PythonRepr.of(NODES.booleanNode(true)));
        assertEquals("False", PythonRepr.of(NODES.booleanNode(false)));
    }

    @Test
    void formatsStringsWithSingleQuotes() {
        assertEquals("'150'", PythonRepr.of(NODES.textNode("150")));
    }

    @Test
    void formatsIntegersWithoutDecimalPoint() {
        assertEquals("150", PythonRepr.of(NODES.numberNode(150)));
    }

    @Test
    void formatsNonFiniteDoubles() throws Exception {
        ArrayNode flows = MAPPER.createArrayNode();
        flows.add(Double.POSITIVE_INFINITY);
        flows.add(Double.NaN);
        assertEquals("inf", PythonRepr.of(flows.get(0)));
        assertEquals("nan", PythonRepr.of(flows.get(1)));
    }

    @Test
    void formatsListsLikePython() {
        ArrayNode flows = NODES.arrayNode().add(125).add(156.25);
        assertEquals("[125, 156.25]", PythonRepr.of(flows));
        assertEquals("[]", PythonRepr.of(NODES.arrayNode()));
    }
}
