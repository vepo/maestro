package dev.vepo.maestro.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class MaestroTable {
    public class MaestroValue {
        private final String key;

        private MaestroValue(String key) {
            this.key = key;
        }

        /**
         * Set the value for the previously specified key
         */
        public MaestroTable value(String value) {

            if (state == TableState.CREATED) {
                state = TableState.POPULATING;
            }

            if (state != TableState.POPULATING && state != TableState.READY) {
                throw new IllegalStateException("Cannot add values to table in state: " + state);
            }

            data.put(key, value);
            return MaestroTable.this;
        }
    }

    /**
     * Table states for lifecycle management
     */
    public enum TableState {
        CREATED("Table created but not populated"),
        POPULATING("Table is being populated with data"),
        READY("Table is ready for queries");

        private final String description;

        TableState(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
    private final String name;

    private final Map<String, String> data;

    private TableState state;

    public MaestroTable(String name) {
        this.name = Objects.requireNonNull(name, "Table name cannot be null");
        this.data = new ConcurrentHashMap<>();
        this.state = TableState.CREATED;
    }

    /**
     * Set the key for the next value operation
     */
    public MaestroValue withKey(String key) {
        return new MaestroValue(Objects.requireNonNull(key, "Key cannot be null"));
    }

    /**
     * Finalize table creation and make it available for queries
     */
    public void create() {
        if (state == TableState.CREATED && data.isEmpty()) {
            // Empty table is valid
            state = TableState.READY;
        } else if (state == TableState.POPULATING) {
            state = TableState.READY;
        } else if (state == TableState.READY) {
            throw new IllegalStateException("Table '" + name + "' is already created");
        } else {
            throw new IllegalStateException("Cannot create table in state: " + state);
        }

        System.out.println("Table '" + name + "' created with " + data.size() + " entries");
    }

    /**
     * Look up a value by key (used by the engine during joins)
     */
    public String lookup(String key) {
        if (state != TableState.READY) {
            throw new IllegalStateException("Table '" + name + "' is not ready. Current state: " + state);
        }
        return data.get(key);
    }

    /**
     * Check if the table contains a key
     */
    public boolean containsKey(String key) {
        if (state != TableState.READY) {
            throw new IllegalStateException("Table '" + name + "' is not ready. Current state: " + state);
        }
        return data.containsKey(key);
    }

    /**
     * Get the size of the table
     */
    public int size() {
        return data.size();
    }

    /**
     * Clear all data from the table
     */
    public void clear() {
        data.clear();
        state = TableState.CREATED;
    }

    /**
     * Get table name
     */
    public String getName() {
        return name;
    }

    /**
     * Get table state
     */
    public TableState getState() {
        return state;
    }

    /**
     * Bulk load multiple entries at once
     */
    public MaestroTable load(Map<String, String> entries) {
        if (state == TableState.CREATED) {
            state = TableState.POPULATING;
        }

        if (state != TableState.POPULATING) {
            throw new IllegalStateException("Cannot load entries to table in state: " + state);
        }

        data.putAll(entries);
        return this;
    }

    /**
     * Remove a specific key from the table
     */
    public MaestroTable remove(String key) {
        if (state != TableState.READY) {
            throw new IllegalStateException("Cannot remove from table in state: " + state);
        }
        data.remove(key);
        return this;
    }

    /**
     * Update an existing key with new value
     */
    public MaestroTable update(String key, String value) {
        if (state != TableState.READY) {
            throw new IllegalStateException("Cannot update table in state: " + state);
        }
        if (!data.containsKey(key)) {
            throw new IllegalArgumentException("Key '" + key + "' does not exist in table");
        }
        data.put(key, value);
        return this;
    }

    /**
     * Check if table is ready for queries
     */
    public boolean isReady() {
        return state == TableState.READY;
    }

    /**
     * Get a copy of all data (for testing/debugging)
     */
    public Map<String, String> getAllData() {
        return new HashMap<>(data);
    }

    @Override
    public String toString() {
        return String.format("MaestroTable{name='%s', state=%s, size=%d}",
                             name, state, data.size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MaestroTable that = (MaestroTable) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}