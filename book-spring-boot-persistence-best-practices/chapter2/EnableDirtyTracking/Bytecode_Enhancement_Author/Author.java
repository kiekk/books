//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.bookstore.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import org.hibernate.bytecode.enhance.internal.bytebuddy.InlineDirtyCheckerEqualsHelper;
import org.hibernate.bytecode.enhance.internal.tracker.DirtyTracker;
import org.hibernate.bytecode.enhance.internal.tracker.NoopCollectionTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleFieldTracker;
import org.hibernate.bytecode.enhance.spi.CollectionTracker;
import org.hibernate.bytecode.enhance.spi.EnhancementInfo;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.internal.util.collections.ArrayHelper;

@Entity
@EnhancementInfo(
        version = "6.5.2.Final"
)
public class Author implements ManagedEntity, PersistentAttributeInterceptable, SelfDirtinessTracker {
    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    Long id;
    String name;
    String genre;
    int age;
    @Transient
    private transient EntityEntry $$_hibernate_entityEntryHolder;
    @Transient
    private transient ManagedEntity $$_hibernate_previousManagedEntity;
    @Transient
    private transient ManagedEntity $$_hibernate_nextManagedEntity;
    @Transient
    private transient boolean $$_hibernate_useTracker;
    @Transient
    private transient PersistentAttributeInterceptor $$_hibernate_attributeInterceptor;
    @Transient
    private transient DirtyTracker $$_hibernate_tracker;

    public Author() {
    }

    public static Author createAuthor(String name, int age, String genre) {
        Author author = new Author();
        author.$$_hibernate_write_name(name);
        author.$$_hibernate_write_age(age);
        author.$$_hibernate_write_genre(genre);
        return author;
    }

    public void changeAge(int age) {
        this.$$_hibernate_write_age(age);
    }

    public String toString() {
        Long var10000 = this.$$_hibernate_read_id();
        return "Author{id=" + var10000 + ", name='" + this.$$_hibernate_read_name() + "', genre='" + this.$$_hibernate_read_genre() + "', age=" + this.$$_hibernate_read_age() + "}";
    }

    public Object $$_hibernate_getEntityInstance() {
        return this;
    }

    public EntityEntry $$_hibernate_getEntityEntry() {
        return this.$$_hibernate_entityEntryHolder;
    }

    public void $$_hibernate_setEntityEntry(EntityEntry var1) {
        this.$$_hibernate_entityEntryHolder = var1;
    }

    public ManagedEntity $$_hibernate_getPreviousManagedEntity() {
        return this.$$_hibernate_previousManagedEntity;
    }

    public void $$_hibernate_setPreviousManagedEntity(ManagedEntity var1) {
        this.$$_hibernate_previousManagedEntity = var1;
    }

    public ManagedEntity $$_hibernate_getNextManagedEntity() {
        return this.$$_hibernate_nextManagedEntity;
    }

    public void $$_hibernate_setNextManagedEntity(ManagedEntity var1) {
        this.$$_hibernate_nextManagedEntity = var1;
    }

    public boolean $$_hibernate_useTracker() {
        return this.$$_hibernate_useTracker;
    }

    public void $$_hibernate_setUseTracker(boolean var1) {
        this.$$_hibernate_useTracker = var1;
    }

    public PersistentAttributeInterceptor $$_hibernate_getInterceptor() {
        return this.$$_hibernate_attributeInterceptor;
    }

    public void $$_hibernate_setInterceptor(PersistentAttributeInterceptor var1) {
        this.$$_hibernate_attributeInterceptor = var1;
    }

    public void $$_hibernate_trackChange(String var1) {
        if (this.$$_hibernate_tracker == null) {
            this.$$_hibernate_tracker = new SimpleFieldTracker();
        }

        this.$$_hibernate_tracker.add(var1);
    }

    public String[] $$_hibernate_getDirtyAttributes() {
        Object var1 = null;
        String[] var3 = this.$$_hibernate_tracker == null ? ArrayHelper.EMPTY_STRING_ARRAY : this.$$_hibernate_tracker.get();
        return var3;
    }

    public boolean $$_hibernate_hasDirtyAttributes() {
        boolean var1 = false;
        var1 = this.$$_hibernate_tracker != null && !this.$$_hibernate_tracker.isEmpty();
        return var1;
    }

    public void $$_hibernate_clearDirtyAttributes() {
        if (this.$$_hibernate_tracker != null) {
            this.$$_hibernate_tracker.clear();
        }

    }

    public void $$_hibernate_suspendDirtyTracking(boolean var1) {
        if (this.$$_hibernate_tracker == null) {
            this.$$_hibernate_tracker = new SimpleFieldTracker();
        }

        this.$$_hibernate_tracker.suspend(var1);
    }

    public CollectionTracker $$_hibernate_getCollectionTracker() {
        Object var1 = null;
        CollectionTracker var3 = NoopCollectionTracker.INSTANCE;
        return var3;
    }

    public Long $$_hibernate_read_id() {
        if (this.$$_hibernate_getInterceptor() != null) {
            this.id = (Long)this.$$_hibernate_getInterceptor().readObject(this, "id", this.id);
        }

        return this.id;
    }

    public void $$_hibernate_write_id(Long var1) {
        if (this.$$_hibernate_getInterceptor() != null) {
            this.id = (Long)this.$$_hibernate_getInterceptor().writeObject(this, "id", this.id, var1);
        } else {
            this.id = var1;
        }
    }

    public String $$_hibernate_read_name() {
        if (this.$$_hibernate_getInterceptor() != null) {
            this.name = (String)this.$$_hibernate_getInterceptor().readObject(this, "name", this.name);
        }

        return this.name;
    }

    public void $$_hibernate_write_name(String var1) {
        if (!InlineDirtyCheckerEqualsHelper.areEquals(this, "name", var1, this.name)) {
            this.$$_hibernate_trackChange("name");
        }

        if (this.$$_hibernate_getInterceptor() != null) {
            this.name = (String)this.$$_hibernate_getInterceptor().writeObject(this, "name", this.name, var1);
        } else {
            this.name = var1;
        }
    }

    public String $$_hibernate_read_genre() {
        if (this.$$_hibernate_getInterceptor() != null) {
            this.genre = (String)this.$$_hibernate_getInterceptor().readObject(this, "genre", this.genre);
        }

        return this.genre;
    }

    public void $$_hibernate_write_genre(String var1) {
        if (!InlineDirtyCheckerEqualsHelper.areEquals(this, "genre", var1, this.genre)) {
            this.$$_hibernate_trackChange("genre");
        }

        if (this.$$_hibernate_getInterceptor() != null) {
            this.genre = (String)this.$$_hibernate_getInterceptor().writeObject(this, "genre", this.genre, var1);
        } else {
            this.genre = var1;
        }
    }

    public int $$_hibernate_read_age() {
        if (this.$$_hibernate_getInterceptor() != null) {
            this.age = this.$$_hibernate_getInterceptor().readInt(this, "age", this.age);
        }

        return this.age;
    }

    public void $$_hibernate_write_age(int var1) {
        if (!InlineDirtyCheckerEqualsHelper.areEquals(this, "age", var1, this.age)) {
            this.$$_hibernate_trackChange("age");
        }

        if (this.$$_hibernate_getInterceptor() != null) {
            this.age = this.$$_hibernate_getInterceptor().writeInt(this, "age", this.age, var1);
        } else {
            this.age = var1;
        }
    }
}
