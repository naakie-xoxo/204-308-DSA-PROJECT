package ug.edu.ugmc.optimizer.datastructures.hashing;

/**
 * Project-owned hash set backed by {@link CustomHashTable}.
 *
 * <p>Each unique value is stored as a key with a private marker. CustomSet
 * deliberately rejects null values for all set operations, providing one
 * consistent set-level null policy.</p>
 *
 * @param <T> value type stored by the set
 */
public class CustomSet<T> {

    private enum Marker {
        PRESENT
    }

    private final CustomHashTable<T, Marker> entries;

    /** Creates an empty set using the hash table's index-derived capacity. */
    public CustomSet() {
        entries = new CustomHashTable<>();
    }

    /**
     * Adds a value if it is not already present.
     *
     * @param value non-null value to add
     * @return {@code true} when inserted; {@code false} for a duplicate
     * @throws IllegalArgumentException if {@code value} is null
     */
    public boolean add(T value) {
        requireNonNull(value);
        if (entries.get(value) != null) {
            return false;
        }
        entries.put(value, Marker.PRESENT);
        return true;
    }

    /**
     * Reports whether a value belongs to the set.
     *
     * @param value non-null value to query
     * @return {@code true} when the value is present
     * @throws IllegalArgumentException if {@code value} is null
     */
    public boolean contains(T value) {
        requireNonNull(value);
        return entries.get(value) != null;
    }

    /**
     * Removes a value when present.
     *
     * @param value non-null value to remove
     * @return {@code true} when removed; {@code false} when it was absent
     * @throws IllegalArgumentException if {@code value} is null
     */
    public boolean remove(T value) {
        requireNonNull(value);
        if (entries.get(value) == null) {
            return false;
        }
        entries.remove(value);
        return true;
    }

    /** @return the number of unique values in the set */
    public int size() {
        return entries.getSize();
    }

    /** @return whether the set contains no values */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    private static void requireNonNull(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Custom set values cannot be null.");
        }
    }
}
