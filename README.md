# Axon Configuration Library
[![Build Status](https://travis-ci.org/AITestingOrg/library--axon-configuration.svg?branch=master)](https://travis-ci.org/AITestingOrg/library--axon-configuration) [ ![Download](https://api.bintray.com/packages/aitestingorg/EventSourcingConfiguration/library--axon-configuration/images/download.svg) ](https://bintray.com/aitestingorg/EventSourcingConfiguration/library--axon-configuration/_latestVersion)

Easily configure Axon event sourcing and CQRS.

## Usage
The library contains several AMQP Axon Configuration classes which can be extended from.

### Gradle Configuration
Coming soon!
```groovy
dependencies {
	compile('org.aist.libs.eventsourcing.configuration')
}
```

### Setting up an Event Listener Application.
To setup a event listener two files are required, a configuration class that extends the AmqpEventSubscriptionConfiguration and an application configuration.

#### Example Configuration Class
```java
@Configuration
public class MyAmqpEventConfiguration extends AmqpEventSubscriptionConfiguration {}
```

#### Example Application Configuration
```yaml
amqp:
    events:
      host: <RabbitMQ Hostname>
      exchange-name: <RabbitMQ Exchange Name>
      queue-name: <RabbitMQ Queue Name>
      handlers: <Package name with EventHandlers (com.my.package)>
```

#### Example Usage
```java
@Component
public class MyEventHandler {
    protected static final Logger LOG = LoggerFactory.getLogger(MyEventHandler.class);

    @Autowired
    MyRepository MyRepository;

    @EventHandler
    public void handle(MyCreatedEvent event) {
        LOG.info(String.format("My Created: %s %s ID: %s", event.getFirstName(), event.getLastName(), event.getId()));
        MyRepository.save(new My(event.getId(), event.getFirstName(), event.getLastName()));
    }

    @EventHandler
    public void handle(MyUpdatedEvent event) {
        LOG.info(String.format("My Updated: %s %s ID: %s", event.getFirstName(), event.getLastName(), event.getId()));
       MyRepository.save(new My(event.getId(), event.getFirstName(), event.getLastName()));
    }

    @EventHandler
    public void handle(MyDeletedEvent event) {
        LOG.info(String.format("My Deleted %s", event.getId()));
        MyRepository.delete(event.getId().toString());
    }
}
```

### Setting up an Event Publisher, Commmand Bus, EventStore Application
The publisher requires three files to be configured properly. These include an application configuration file, a configuration class that extends AmqpEventPublisherConfiguration, and an implementation of MyCommandHandler.
#### Example Configuration Class
The type of the desired aggregate must be provided in the constructor and the abstract method commandHandler must be implemented.
The implementation of CommandHandler allows for custom subscriptions and customizations on the listener attatched to the CommandBus.
```java
@Configuration
public class MyEventStoreConfiguration extends AmqpEventPublisherConfiguration<MyAggregate, MyCommandHandler> {
    public MyEventStoreConfiguration() {
        super(MyAggregate.class);
    }

    @Override
    @Bean
    public MyCommandHandler commandHandler(EventSourcingRepository eventSourcingRepository, CommandBus commandBus) {
        MyCommandHandler commandHandler = new MyCommandHandler(eventSourcingRepository, commandBus);
        commandHandler.subscribe();
        return commandHandler;
    }
}
```
#### Example Application Configuration
```yaml
axon:
  eventstore:
    mongo:
      connections:
        default:
          uri: <Event Store Mongo URI>
          aggregates:
            - MyAggregate
amqp:
    events:
      host: <RabbitMQ Hostname>
      exchange-name: <RabbitMQ Exchange Name>
      queue-name: <RabbitMQ Queue Name>
      handlers: <Package name with EventHandlers (com.my.package)>
```

#### Example Command Handler
The default implementation provided by CustomCommandHandler utilizes AggregateAnnotationCommandHandler to subscribe to the CommandBus. This can be overided for which ever listener desired.
```java
public class MyCommandHandler extends CustomCommandHandler<MyAggregate> {
    public MyCommandHandler(EventSourcingRepository repository, CommandBus commandBus) {
        super(repository, commandBus, MyAggregate.class);
    }
}
```

#### Example Usage
```java
@Aggregate
public class MyAggregate {

    @AggregateIdentifier
    private UUID id;
    @NotNull
    private String firstName;
    @NotNull
    private String lastName;

    @CommandHandler
    public MyAggregate(CreateMyAggregateCommand createMyAggregateCommand) {
        apply(new MyAggregateCreatedEvent(createMyAggregateCommand.getId(), createMyAggregateCommand.getFirstName(), createMyAggregateCommand.getLastName()));
    }

    @EventSourcingHandler
    public void on(MyAggregateCreatedEvent event) {
        id = event.getId();
        firstName = event.getFirstName();
        lastName = event.getLastName();
    }

    public MyAggregate(UUID id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public MyAggregate() {}

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @CommandHandler
    public void on(DeleteMyAggregateCommand command) {
        apply(new MyAggregateDeletedEvent(id));
    }

    @CommandHandler
    public void on(UpdateMyAggregateCommand command) {
        apply(new MyAggregateUpdatedEvent(command.getId(), command.getFirstName(), command.getLastName()));
    }

    @EventSourcingHandler
    public void on(MyAggregateDeletedEvent event) {
        markDeleted();
    }

    @EventSourcingHandler
    public void on(MyAggregateUpdatedEvent event) {
        firstName = event.getFirstName();
        lastName = event.getLastName();
    }
}
```