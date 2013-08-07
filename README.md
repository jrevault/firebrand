[![Build Status](https://travis-ci.org/47deg/firebrand.png?branch=master)](undefined)

# Firebrand OCM 

Firebrand OCM is a simple library for persisting and querying [Java](http://en.wikipedia.org/wiki/Java_language)
Objects to a Cassandra Database. Firebrand's goal is to provide an elegant and simple interface to bring the power and scalability
of [Apache Cassandra](http://cassandra.apache.org/) to your application.


# Download

## Maven Dependency

Firebrand may be automatically imported into your project if you already use [Maven](http://maven.apache.org/).
Just declare Firebrand as a maven dependency. If you wish to always use the latest unstable snapshots, add the Sonatype
repository where the Firebrand snapshot artifacts are being deployed. Firebrand official releases will be made available at Maven Central.

```xml
<repository>
    <id>sonatype</id>
    <url>https://oss.sonatype.org/content/groups/public/</url>
    <releases>
        <enabled>true</enabled>
        <updatePolicy>daily</updatePolicy>
        <checksumPolicy>fail</checksumPolicy>
    </releases>
    <snapshots>
        <enabled>true</enabled>
        <updatePolicy>always</updatePolicy>
        <checksumPolicy>ignore</checksumPolicy>
    </snapshots>
</repository>

<dependency>
    <groupId>org.firebrandocm</groupId>
    <artifactId>firebrand</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```
## JAR and others

Latest snapshots in jar, javadoc and sources forms are published to the sonatype snapshot repository [here](https://oss.sonatype.org/content/repositories/snapshots/org/firebrandocm/firebrand/1.0-SNAPSHOT/)

# Usage

## Persistence Factory

Access to the persistence operations and all entity related operations are performed via the [PersistenceFactory](https://github.com/47deg/firebrand/blob/master/src/main/java/org/firebrandocm/dao/PersistenceFactory.java).
The persistence factory includes methods to perform the most common operations for creating, persisting,
removing and querying the entities persisted in the underlying Cassandra database.

### Configuration

Firebrand ships with an implementation of the [PersistenceFactory](https://github.com/47deg/firebrand/blob/master/src/main/java/org/firebrandocm/dao/PersistenceFactory.java) based on the popular Cassandra client [Hector](http://hector-client.org).
You can configure an instance of the [HectorPersistenceFactory](https://github.com/47deg/firebrand/blob/master/src/main/java/org/firebrandocm/dao/impl/HectorPersistenceFactory.java) programmatically or with an IOC container such as [Spring](http://www.springsource.org/).
The Persistence Factory is ment to be cached and reused throught the application lifecycle.

#### Spring

```xml
<bean class="org.firebrandocm.dao.impl.hector.HectorPersistenceFactory" init-method="init" destroy-method="destroy">
        <property name="defaultConsistencyLevel" value="ANY" />
        <property name="clusterName" value="${cassandra.cluster}" />
        <property name="defaultKeySpace" value="${cassandra.keyspace}" />
        <property name="contactNodes" value="${cassandra.rpc.addresses}"/>
        <property name="thriftPort" value="${cassandra.thrift.port}" />
        <property name="autoDiscoverHosts" value="${cassandra.autoDiscoverHosts}" />
        <property name="entities">
            <list>
                <value>com.yourcompany.domain.Entity</value>
                ...
            </list>
        </property>
    </bean>
```

A handy way of handling multiple entities without declaring each exists. Declare a base package where all classes will be added to your PersistenceFactory :

```xml
         <property name="entitiesPkg" value="com.yourcompany.domain">
```

```java
@Autowired
private PersistenceFactory persistenceFactory
```

#### Programmatically

```java
PersistenceFactory persistenceFactory = new HectorPersistenceFactory.Builder()
                    .defaultConsistencyLevel(true)
                    .clusterName(cluster)
                    .defaultKeySpace(keySpace)
                    .contactNodes(nodes)
                    .thriftPort(port)
                    .autoDiscoverHosts(autoDiscoverHosts)
                    .entities(entities)
                    .build();
```

### Get

Load any entity from the data store by key.

```java
Entity entity = peristenceFactory.get(Entity.class, key);
```

### Persist

Persist one or multiple entities.

```java
Entity entity = ...;
entity.setName(name);
persistenceFactory.persist(entity)
```

```java
Entity one = ...;
Entity two = ...;
Entity three = ...;
persistenceFactory.persist(one, two, three, ...);
```

### Remove

Remove one or multiple entities.

```java
Entity entity = ...
persistenceFactory.remove(entity)
```

```java
Entity one = ...;
Entity two = ...;
Entity three = ...;
persistenceFactory.remove(one, two, three, ...);
```

### Query

Firebrand supports [CQL](http://cassandra.apache.org/doc/cql/CQL.html) to fetch managed entities.
The Persistence Factory understands both [**Named Queries**](#named-queries) and [Query](https://github.com/47deg/firebrand/blob/master/src/main/java/org/firebrandocm/dao/Query.java) statements generated by the [QueryBuilder](#query-builder).

#### Query Builder

A [Query Builder](#query-builder) is included with support for most of the statements and clauses supported by CQL that may be used
to construct a Query. The Query Builder acts as a static builder that may be imported statically for quick and easy syntactic access to all its methods.

```java
import static org.firebrandocm.dao.cql.QueryBuilder.*;
...
List<Account> accounts = factory.getResultList(Account.class, Query.get(select(allColumns(), from(Account.class))));
```

#### Named Queries

Named queries are loaded when the PersistenceFactory is initialized and can be referenced by name.
Named queries are declared using [@NamedQuery](#@namedquery) and support named parameters in the format of *:parameter*.

```java
@ColumnFamily
@NamedQueries({
		@NamedQuery(name = Account.QUERY_ALL_ACCOUNTS_WITH_NAME, query = "select * from Account where name = :name")
})
public class Account {

	public static final String QUERY_ALL_ACCOUNTS_WITH_NAME = "Account.QUERY_ALL_ACCOUNTS_WITH_NAME";

    @Key
    private String key;

	@Column(indexed = true)
    private String name;

    ... getters & setters

}
```

```java
List<Account> accounts = factory.getResultList(Account.class, Query.get(Account.QUERY_ALL_ACCOUNTS_WITH_NAME, new HashMap<String, Object>(){{
    put("name", name);
}}));
```

### Enhanced instances

Firebrand can increase performance and give you better control on how data is loaded at runtime if it knows when you are
going to perform certain operations. For example, Firebrand can lazy load properties only when you invoke their getter instead of preloading
all persistent properties and hidrating your model eagerly. In order to perform this operation Firebrand uses [Javassist](http://www.javassist.org)
based proxies of your entities that provide advice around invokation of certain methods.
In the rare event that you need to manually obtain proxies up front you can directly invoke [org.firebrandocm.dao.PersistenceFactory#getInstance(Class<Entity>)]()

## Annotations

Firebrand is an annotation based framework. Most annotations are declared directly in the classes that represent persistent entities.

### @ColumnFamily

The Class *@ColumnFamily* annotation declares a class as persistent. Each instance of this class will have its [columns](#@column) persisted under the
same row.

```java
@ColumnFamily
public class Account {
    ...
}
```

### @Key



### @Column

The field *@Column* annotation declares a field as a column. All fields are implicit considered as columns even when they
are not annotated as such. To selectively ignore fields see [Transient](#@transient).

```java
@ColumnFamily
public class Account {

    @Key
    private String key;

    @Column(indexed = true)
    private String name;

    ...

}
```
Columns may be configured to affect they way the interact with the PersistenceFactory and the underlying Cassandra database.
Some of the most common configuration properties for columns are as follows:

* indexed - columns used in queries with eq, lt, ... must be indexed
* lazy - wether this column's value will be loaded eagerly or lazily loaded when it's accesor is invoked.
* validationClass - the validator used by cassandra when manipulating data
* counter - if this column represents a counter type column
* indexType - the type of index for the column

### @CounterIncrease

Fields annotated with *@CounterIncrease* are evaluated to have a column with counter type column
increased or decreased on a persistence operation.

```java
@ColumnFamily(defaultValidationClass = CounterColumnType.class)
public class CounterEntity {

	@Key
	private String key;

	@Column(counter = true, validationClass = CounterColumnType.class)
	private long counterProperty;

	@CounterIncrease("counterProperty")
	private long counterPropertyIncreaseBy;

	...
}
```

### @Embedded

Firebrand supports embedded classes.
Embedded entities are classes that do not represent a @ColumnFamily on its own and get flatten into the column family
where they are declared.
Inner properties are persisted following a dot notation key as columns keys. e.g. credentials.password
Multiple levels of embedded properties are supported.

```java
public class Credentials {

    private String password;

    ...

}
```

```java
@ColumnFamily
public class Account {

    @Key
    private String key;

    @Embedded
    private Credentials credentials;

    ...

}
```

### @Mapped

Mapped properties represent a *to-one relationship.

```java
@ColumnFamily
public class Account {

    @Key
    private String key;

    ...

}
```

```java
@ColumnFamily
public class Profile {

    @Key
    private String key;

    @Mapped
    private Account account;

    ...

}
```

### @MappedCollection

Mapped collections represent a reference *to-many relationship.

```java
@ColumnFamily
public class Account {

    @Key
    private String key;

    ...

}
```

```java
@ColumnFamily
public class Department {

    @Key
    private String key;

    @MappedCollection
    private List<Account> accounts;

    ...

}
```

### @OnEvent

The special annotation [OnEvent](https://github.com/47deg/firebrand/blob/master/src/main/java/org/firebrandocm/dao/annotations/OnEvent.java) declares an entity method as event listener for the [Entity](#entity-events) and [Column](#column-events) Events broadcasted
by the PersistenceFactory.

```java
@ColumnFamily
public class Account {

    @Key
    private String key;

    @OnEvent(Event.Entity.PRE_PERSIST)
    public void onPrePersist() {
        //do something interesting here
    }

    ...

}
```

### @Transient

Fields declared as *@Transient* will be ignored for any persistence purposes.

```java
@ColumnFamily
public class Account {

    @Key
    private String key;

    @Transient
    private String hairColor;

    ...

}
```

## Events

The Persisence Factory is responsible for broadcasting events when certain operations are performed on entities and columns.
See [@OnEvent](#onevent) for more information on how to subscribe to a given event.
The following is a list for both Entity and Column events.

### Entity Events

* PRE_DELETE
* POST_DELETE
* PRE_LOAD
* POST_LOAD
* PRE_PERSIST
* POST_PERSIST
* POST_COMMIT

### Column Events

* PRE_COLUMN_MUTATION
* PRE_COUNTER_MUTATION
* POST_COLUMN_MUTATION
* POST_COUNTER_MUTATION
* PRE_COLUMN_DELETION
* POST_COLUMN_DELETION

## Type Converters

Type converters are in charge of converting from Java objects to ByteBuffer and back.
Firebrand ships with type converters for the most common data types.
All type converters implement [org.firebrandocm.dao.TypeConverter](https://github.com/47deg/firebrand/blob/master/src/main/java/org/firebrandocm/dao/TypeConverter.java).
You may contibute new type converters or override the existing ones to the converters map at
org.firebrandocm.dao.AbstractPersistenceFactory#getTypeConverters

## CQL

Firebrand queries are [CQL](http://cassandra.apache.org/doc/cql/CQL.html) queries.
Firebrand supports both pre typed [Named Queries](#named-queries) and a dynamic [Query Builder](#query-builder) that makes building CQL queries programmatically
a breeze.

### Query Builder

Import the query builder static methods to avoid verbose statements.

```java
import static org.firebrandocm.dao.cql.QueryBuilder.*;
```

Chain statements as needed.

```java
select(
    allColumns(),
	from("Account"),
	where(
		eq("name", "test"),
		eq("username", "test2")
	)
)
```

```sql
SELECT * FROM Account WHERE 'name' = 'test' AND 'username' = 'test2';
```

### SELECT

```java
select(
    allColumns(),
	from("Account"),
	where(
		eq("name", "test"),
		eq("username", "test2")
	)
)
```

```sql
SELECT * FROM Account WHERE 'name' = 'test' AND 'username' = 'test2';
```

http://cassandra.apache.org/doc/cql/CQL.html#SELECT

#### Specifying Columns

*All columns*

```java
select(
    allColumns(),
	from("Account")
)
```

```sql
SELECT * FROM Account;
```

*First N*

```java
select(
    first(5),
    reversed(),
	from("Account")
)
```

```sql
SELECT FIRST 5 REVERSED FROM Account;
```

*Range*

```java
select(
    columnRange("a", "z"),
	from("Account")
)
```

```sql
SELECT 'a'..'z' FROM Account;
```

*List*

```java
select(
    columns("a","b","c"),
	from("Account")
)
```

```sql
SELECT 'a','b','c' FROM Account;
```

http://cassandra.apache.org/doc/cql/CQL.html#SpecifyingColumns

#### Column Family

```java
select(
    allColumns(),
	from("Account")
)
```

```sql
SELECT * FROM Account;
```

http://cassandra.apache.org/doc/cql/CQL.html#ColumnFamily

#### Consistency Level

```java
select(
    allColumns(),
	from("Account"),
	consistency(ConsistencyType.ONE)
)
```

```sql
SELECT * FROM Account USING CONSISTENCY ONE;
```

http://cassandra.apache.org/doc/cql/CQL.html#ConsistencyLevel

#### Filtering rows

The WHERE clause support filters for the rows that appear in results.
The supported operator are =, >, >=, <, <=.

```java
select(
    allColumns(),
	from("Account"),
	where(
	    eq("a", 4),
        lt("b", "test"),
        lte("c", 0),
        gt("d", -234),
        gte("e", -92334),
        between("f", 1, 10),
        keyIn(0, 1, 2, 3)
	)
)
```

```sql
SELECT * FROM Account
    WHERE 'a' = '4' AND 'b' < 'test' AND 'c' <= '0' AND 'd' > '-234' AND 'e' >= '-92334'
        AND 'f' >= '1' AND 'f' <= '10' AND KEY in ('0', '1', '2', '3');",
```

http://cassandra.apache.org/doc/cql/CQL.html#Filteringrows

#### Limits

The number of rows returned in a result may be limited with the LIMIT kewyord. If not specified the implicit limit is 10000.

```java
select(
    allColumns(),
	from("Account"),
	limit(10)
)
```

```sql
SELECT * FROM Account LIMIT 10;
```

http://cassandra.apache.org/doc/cql/CQL.html#Limits

### INSERT

```java
insert(
    columnFamily("Account"),
    into("KEY", "a", "b", "c"),
    values(1, 0, 1, 2),
	writeOptions(
        consistency(ConsistencyType.ONE),
        ttl(86400)
    )
)
```

```sql
INSERT INTO Account (KEY, 'a', 'b', 'c') VALUES ('1', '0', '1', '2') USING CONSISTENCY ONE AND TTL 86400;
```

http://cassandra.apache.org/doc/cql/CQL.html#INSERT

### UPDATE

```java
update(
    columnFamily("Account"),
    writeOptions(
        consistency(ConsistencyType.ONE),
        timestamp(546745),
        ttl(34352)
    ),
    set(
        assign("a", 4),
        assign("b", "test"),
		assign("c", 0)
	),
    where(
        key(4)
    )
)
```

```sql
UPDATE Account USING CONSISTENCY ONE AND TIMESTAMP 546745 AND TTL 34352 SET 'a' = '4', 'b' = 'test', 'c' = '0' WHERE KEY = '4';
```

http://cassandra.apache.org/doc/cql/CQL.html#update

### DELETE

```java
delete(
    columns("a", "b", "c"),
	from("Account"),
	writeOptions(
		consistency(ConsistencyType.ONE),
		timestamp(21342134)
	),
	where(
	    keyIn(0, 1, 2, 3)
	)
)
```

```sql
DELETE 'a', 'b', 'c' FROM Account USING CONSISTENCY ONE AND TIMESTAMP 21342134 WHERE KEY in ('0', '1', '2', '3');
```

http://cassandra.apache.org/doc/cql/CQL.html#DELETE


### TRUNCATE

```java
delete(
	columnFamily("Account")
)
```

```sql
TRUNCATE Account;
```

http://cassandra.apache.org/doc/cql/CQL.html#TRUNCATE

### BATCH

```java
batch(
    writeOptions(
		consistency(ConsistencyType.ONE),
		ttl(3600),
		timestamp(2342134)
	),
	delete(
		columns("a", "b", "c"),
		from("ColumnFamily")
	),
	insert(
	    columnFamily("Account"),
		into("a", "b", "c"),
		values(0, 1, 2)
    ),
	update(
	    columnFamily("ColumnFamily"),
		set(
		    assign("propertya", 4)
		),
		where(
		    key(4)
		)
	)
)
```

```sql
BEGIN BATCH USING CONSISTENCY ONE AND TTL 3600 AND TIMESTAMP 2342134
DELETE 'a', 'b', 'c' FROM ColumnFamily;
INSERT INTO Account ('a', 'b', 'c') VALUES ('0', '1', '2');
UPDATE ColumnFamily SET 'propertya' = '4' WHERE KEY = '4';
APPLY BATCH;
```

http://cassandra.apache.org/doc/cql/CQL.html#BATCH


### CREATE KEYSPACE

```java
createKeyspace(
	keySpace("KeySpaceName"),
    withStrategyClass(SimpleStrategy.class),
	strategyOptions(
		replicationFactor(1)
	)
)
```

```sql
CREATE KEYSPACE KeySpaceName WITH strategy_class = SimpleStrategy AND strategy_options:replication_factor = 1;
```

http://cassandra.apache.org/doc/cql/CQL.html#CREATEKEYSPACE

### CREATE COLUMNFAMILY

```java
createColumnFamily(
    columnFamily("ColumnFamilyName"),
	columnDefinitions(
		primaryKey("primaryKeyColumn", ColumnDataType.UUID),
		column("a", ColumnDataType.TEXT),
		column("b", ColumnDataType.INT)
	),
	storageOptions(
		storageOption(StorageParameter.COMMENT, "comment"),
		storageOption(StorageParameter.READ_REPAIR_CHANCE, "1")
	)
)
```

```sql
CREATE COLUMNFAMILY ColumnFamilyName (
    'primaryKeyColumn' uuid PRIMARY KEY,
	'a' text,
	'b' int
) WITH
    comment = comment AND
    read_repair_chance = 1;
```

http://cassandra.apache.org/doc/cql/CQL.html#CREATECOLUMNFAMILY

### CREATE INDEX

```java
createIndex(
    onColumnFamily("ColumnFamily"),
	indexName("myIndex"),
	column("myColumn")
)
```

```sql
CREATE INDEX myIndex ON ColumnFamily ('myColumn');
```

http://cassandra.apache.org/doc/cql/CQL.html#CREATEINDEX

### DROP KEYSPACE

```java
drop(
    keySpace("KeySpaceName")
)
```

```sql
DROP KEYSPACE KeySpaceName;
```

http://cassandra.apache.org/doc/cql/CQL.html#DROPKEYSPACE

### DROP COLUMNFAMILY

```java
drop(
    columnFamily("ColumnFamilyName")
)
```

```sql
DROP COLUMNFAMILY ColumnFamilyName;
```

http://cassandra.apache.org/doc/cql/CQL.html#DROPCOLUMNFAMILY

### DROP INDEX

```java
drop(
    indexName("myIndex")
)
```

```sql
DROP INDEX myIndex;
```

http://cassandra.apache.org/doc/cql/CQL.html#DROPINDEX

### ALTER COLUMNFAMILY

```java
alterColumnFamily(
    columnFamily("ColumnFamily"),
	add("myColumn", ColumnDataType.INT)
)
```

```sql
ALTER COLUMNFAMILY ColumnFamily ADD 'myColumn' int;
```

http://cassandra.apache.org/doc/cql/CQL.html#ALTERCOLUMNFAMILY

### Value Converters

The Firebrand CQL Query Builder comes with a few utility converters that convert some common data types in their
CQL value as mapped by Firebrand.

All CQL value converters are implementers of [org.firebrandocm.dao.cql.converters.CQLValueConverter](https://github.com/47deg/firebrand/blob/master/src/main/java/org/firebrandocm/dao/cql.converters/CQLValueConverter.java).

You may contribute your own converters or override the default configured ones with
org.cassandraobjects.dao.cql.QueryBuilder#addConverter
