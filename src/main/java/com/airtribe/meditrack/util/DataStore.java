package com.airtribe.meditrack.util;

import com.airtribe.meditrack.entity.MedicalEntity;
import com.airtribe.meditrack.exception.DataPersistenceException;
import com.airtribe.meditrack.exception.EntityNotFoundException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A type-safe, in-memory repository for any {@link MedicalEntity}.
 *
 * <h2>Why generics</h2>
 * <p>Without them we would write {@code PatientStore}, {@code DoctorStore} and
 * {@code AppointmentStore} — three copies of identical CRUD logic — or one
 * {@code Map<String, Object>} store that pushes a cast onto every caller and turns a
 * typo into a {@link ClassCastException} at run time. The type parameter
 * {@code <T extends MedicalEntity>} gives us one implementation, checked at compile
 * time, that still knows every element has {@code getId()} and is {@link
 * com.airtribe.meditrack.interfaces.Searchable}.</p>
 *
 * <p>The {@code extends MedicalEntity} <b>bound</b> is what makes that work: an unbounded
 * {@code <T>} would leave the compiler unable to prove {@code getId()} exists.</p>
 *
 * <h2>Iteration</h2>
 * <p>Implements {@link Iterable}, so a store works directly in a for-each loop. A hand-written
 * {@link Iterator} is supplied rather than delegating, to demonstrate the contract — and
 * it deliberately refuses {@code remove()} so callers cannot mutate the store mid-traversal.</p>
 *
 * @param <T> the entity type held by this store
 * @author Sunil (Utils, Storage, Singleton, Docs and Testing)
 */
public class DataStore<T extends MedicalEntity> implements Iterable<T>, Serializable {

    private static final long serialVersionUID = 1L;

    /** {@link LinkedHashMap} so iteration order is insertion order — stable, predictable output. */
    private final Map<String, T> storage;

    /** Label used in error messages, e.g. {@code "Patient"}. */
    private final String entityLabel;

    /**
     * @param entityLabel human-readable name of the entity type held here
     */
    public DataStore(String entityLabel) {
        this.storage = new LinkedHashMap<>();
        this.entityLabel = entityLabel == null ? "Entity" : entityLabel;
    }

    // ---------------------------------------------------------------------- CRUD
    /**
     * Inserts or replaces an entity, keyed on its id.
     *
     * @param entity the entity to store
     * @return the stored entity, for call chaining
     */
    public T save(T entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        storage.put(entity.getId(), entity);
        return entity;
    }

    /**
     * Stores every entity in a collection.
     *
     * @param entities the entities to store
     * @return how many were stored
     */
    public int saveAll(Collection<T> entities) {
        if (entities == null) {
            return 0;
        }
        int before = storage.size();
        entities.forEach(this::save);
        return storage.size() - before;
    }

    /**
     * @param id the id to look up
     * @return the entity, or {@link Optional#empty()} — never {@code null}, so callers
     *         cannot forget the missing case
     */
    public Optional<T> findById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(storage.get(id));
    }

    /**
     * Strict lookup for callers that treat a miss as an error.
     *
     * @param id the id to look up
     * @return the entity
     * @throws EntityNotFoundException if no entity has that id
     */
    public T getById(String id) throws EntityNotFoundException {
        return findById(id).orElseThrow(() -> new EntityNotFoundException(entityLabel, id));
    }

    /** @return a defensive copy of all stored entities, in insertion order */
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    /**
     * @param id the id to remove
     * @return {@code true} if something was removed
     */
    public boolean deleteById(String id) {
        return id != null && storage.remove(id) != null;
    }

    /**
     * @param id the id to check
     * @return {@code true} if an entity with that id exists
     */
    public boolean exists(String id) {
        return id != null && storage.containsKey(id);
    }

    /** @return how many entities are stored */
    public int count() {
        return storage.size();
    }

    public boolean isEmpty() {
        return storage.isEmpty();
    }

    /** Removes everything. */
    public void clear() {
        storage.clear();
    }

    // -------------------------------------------------------------------- query
    /**
     * Filters the store with an arbitrary predicate.
     *
     * @param predicate the test each entity must pass
     * @return the matching entities
     */
    public List<T> findBy(Predicate<T> predicate) {
        if (predicate == null) {
            return findAll();
        }
        return storage.values().stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * Free-text search across every entity's searchable text.
     *
     * <p>Leans on {@link com.airtribe.meditrack.interfaces.Searchable#matches(String)},
     * which the type bound guarantees is available.</p>
     *
     * @param keyword the search term; blank returns everything
     * @return the matching entities
     */
    public List<T> search(String keyword) {
        return storage.values().stream()
                .filter(e -> e.matches(keyword))
                .collect(Collectors.toList());
    }

    /**
     * @param comparator the ordering to apply
     * @return a sorted copy; the store's own order is untouched
     */
    public List<T> findAllSorted(Comparator<T> comparator) {
        List<T> all = findAll();
        all.sort(comparator == null ? Comparator.naturalOrder() : comparator);
        return all;
    }

    /**
     * @return a stream over the stored entities, for callers doing their own analytics
     */
    public Stream<T> stream() {
        return storage.values().stream();
    }

    /**
     * @param predicate the test
     * @return the first matching entity, if any
     */
    public Optional<T> findFirst(Predicate<T> predicate) {
        return storage.values().stream().filter(predicate).findFirst();
    }

    /**
     * @param predicate the test
     * @return how many entities match
     */
    public long countBy(Predicate<T> predicate) {
        return storage.values().stream().filter(predicate).count();
    }

    // ---------------------------------------------------------------- iteration
    /**
     * A read-only iterator over the stored entities.
     *
     * <p>{@link Iterator#remove()} is unsupported on purpose: removing through the
     * iterator would bypass {@link #deleteById(String)} and any future auditing there.</p>
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private final Iterator<T> delegate = new ArrayList<>(storage.values()).iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more " + entityLabel + " records.");
                }
                return delegate.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException(
                        "Remove via DataStore.deleteById(String) instead.");
            }
        };
    }

    // -------------------------------------------------------------- persistence
    /**
     * Writes the whole store to disk using Java serialization.
     *
     * <p>Uses <b>try-with-resources</b>: the streams are closed automatically, in reverse
     * order, even if writing throws. Any {@link IOException} is wrapped in a
     * {@link DataPersistenceException} so the caller sees a domain error while the
     * original cause is preserved.</p>
     *
     * @param filePath where to write
     * @throws DataPersistenceException if the write fails
     */
    public void serializeTo(String filePath) throws DataPersistenceException {
        ensureParentDirectory(filePath);
        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(new ArrayList<>(storage.values()));
        } catch (IOException e) {
            throw new DataPersistenceException(filePath,
                    "Failed to serialize " + entityLabel + " records", e);
        }
    }

    /**
     * Reads a previously serialized store back into memory, replacing current contents.
     *
     * @param filePath where to read from
     * @return how many entities were loaded
     * @throws DataPersistenceException if the file is unreadable or holds the wrong type
     */
    @SuppressWarnings("unchecked")
    public int deserializeFrom(String filePath) throws DataPersistenceException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return 0;
        }
        try (FileInputStream fis = new FileInputStream(filePath);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            Object raw = ois.readObject();
            if (!(raw instanceof List<?> list)) {
                throw new DataPersistenceException(filePath,
                        "Unexpected contents in " + entityLabel + " file", null);
            }
            storage.clear();
            for (Object item : list) {
                T entity = (T) item;   // unchecked: guarded by how we wrote the file
                save(entity);
            }
            return storage.size();

        } catch (IOException | ClassNotFoundException e) {
            throw new DataPersistenceException(filePath,
                    "Failed to deserialize " + entityLabel + " records", e);
        }
    }

    /** Creates the parent directory of a path if it does not yet exist. */
    private static void ensureParentDirectory(String filePath) throws DataPersistenceException {
        try {
            Path parent = Paths.get(filePath).toAbsolutePath().getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new DataPersistenceException(filePath, "Could not create data directory", e);
        }
    }

    public String getEntityLabel() {
        return entityLabel;
    }

    @Override
    public String toString() {
        return "DataStore<" + entityLabel + ">{count=" + storage.size() + '}';
    }
}
