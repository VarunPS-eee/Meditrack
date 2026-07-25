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

public class DataStore<T extends MedicalEntity> implements Iterable<T>, Serializable {

    private static final long serialVersionUID = 1L;

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

    public T save(T entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        storage.put(entity.getId(), entity);
        return entity;
    }

    public int saveAll(Collection<T> entities) {
        if (entities == null) {
            return 0;
        }
        int before = storage.size();
        entities.forEach(this::save);
        return storage.size() - before;
    }

    public Optional<T> findById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(storage.get(id));
    }

    public T getById(String id) throws EntityNotFoundException {
        return findById(id).orElseThrow(() -> new EntityNotFoundException(entityLabel, id));
    }

    /** @return a defensive copy of all stored entities, in insertion order */
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    public boolean deleteById(String id) {
        return id != null && storage.remove(id) != null;
    }

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

    public List<T> findBy(Predicate<T> predicate) {
        if (predicate == null) {
            return findAll();
        }
        return storage.values().stream().filter(predicate).collect(Collectors.toList());
    }

    public List<T> search(String keyword) {
        return storage.values().stream()
                .filter(e -> e.matches(keyword))
                .collect(Collectors.toList());
    }

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

    public Optional<T> findFirst(Predicate<T> predicate) {
        return storage.values().stream().filter(predicate).findFirst();
    }

    public long countBy(Predicate<T> predicate) {
        return storage.values().stream().filter(predicate).count();
    }

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
