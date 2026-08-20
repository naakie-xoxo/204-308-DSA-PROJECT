package ug.edu.ugmc.optimizer.datastructures.hashing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Contract tests for the project-owned custom set. */
class CustomSetTest {

    @Test
    void emptySetHasNoMembers() {
        CustomSet<String> set = new CustomSet<>();

        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
        assertFalse(set.contains("A"));
    }

    @Test
    void addingNewValueCreatesMembership() {
        CustomSet<String> set = new CustomSet<>();

        assertTrue(set.add("A"));
        assertTrue(set.contains("A"));
        assertEquals(1, set.size());
        assertFalse(set.isEmpty());
    }

    @Test
    void duplicateInsertionDoesNotIncreaseSize() {
        CustomSet<String> set = new CustomSet<>();

        assertTrue(set.add("A"));
        assertFalse(set.add("A"));
        assertEquals(1, set.size());
    }

    @Test
    void multipleDistinctValuesRemainPresent() {
        CustomSet<String> set = new CustomSet<>();

        assertTrue(set.add("REQ-001"));
        assertTrue(set.add("REQ-002"));
        assertTrue(set.add("REQ-003"));

        assertTrue(set.contains("REQ-001"));
        assertTrue(set.contains("REQ-002"));
        assertTrue(set.contains("REQ-003"));
        assertEquals(3, set.size());
    }

    @Test
    void serviceRequestCategoriesDemonstrateMembershipAndLookup() {
        CustomSet<String> categories = new CustomSet<>();

        assertTrue(categories.add("Pharmacy"));
        assertTrue(categories.add("Emergency"));
        assertFalse(categories.add("Pharmacy"));
        assertTrue(categories.contains("Pharmacy"));
        assertFalse(categories.contains("Laboratory"));
        assertEquals(2, categories.size());
    }

    @Test
    void removalAffectsOnlyTheSelectedValue() {
        CustomSet<String> set = new CustomSet<>();
        set.add("A");
        set.add("B");

        assertTrue(set.remove("A"));
        assertFalse(set.contains("A"));
        assertTrue(set.contains("B"));
        assertEquals(1, set.size());
        assertFalse(set.isEmpty());

        assertTrue(set.remove("B"));
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
    }

    @Test
    void removingMissingValueLeavesSetUnchanged() {
        CustomSet<String> set = new CustomSet<>();
        set.add("A");

        assertFalse(set.remove("B"));
        assertEquals(1, set.size());
        assertTrue(set.contains("A"));
    }

    @Test
    void distinctCollidingValuesRemainIndependent() {
        CustomSet<Integer> set = new CustomSet<>();

        // The default hash-table capacity is 141, so these integer keys share
        // one separate-chaining bucket.
        assertTrue(set.add(1));
        assertTrue(set.add(142));
        assertTrue(set.add(283));
        assertTrue(set.contains(1));
        assertTrue(set.contains(142));
        assertTrue(set.contains(283));

        assertTrue(set.remove(142));
        assertTrue(set.contains(1));
        assertFalse(set.contains(142));
        assertTrue(set.contains(283));
        assertEquals(2, set.size());
    }

    @Test
    void nullValuesAreRejectedConsistently() {
        CustomSet<String> set = new CustomSet<>();

        assertThrows(IllegalArgumentException.class, () -> set.add(null));
        assertThrows(IllegalArgumentException.class, () -> set.contains(null));
        assertThrows(IllegalArgumentException.class, () -> set.remove(null));
        assertTrue(set.isEmpty());
    }
}
