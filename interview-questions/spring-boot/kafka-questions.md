# Kafka — From Scratch to Interview Level

Simple explanations first, then the interview-style answers (reliability, retries, DLQ, outbox). A practice quiz with answers is at the bottom.

> This repo currently uses **RabbitMQ**, not Kafka (see `spring.rabbitmq.*` in `application.properties`), so nothing here is wired into the code. Pairs with `microservices-communication-questions.md` Q4/Q14 (async communication) and Q19 (choreography).

## 1. What is Kafka, in short?
Kafka is a system where one app **writes messages** and other apps **read them later**. The writer does not call the reader directly. Kafka sits in the middle and **stores** the messages.

Unlike a normal queue, a message is **not deleted after it is read**. Kafka keeps it for a set time (for example 7 days). That is why a new consumer, or a fixed bug, can **replay** old messages.

## 2. What are the core building blocks?
| Concept | What it is |
|---|---|
| **Producer** | The part of your app that sends messages (for example `OrderService`). |
| **Consumer** | The part of your app that reads messages (for example `EmailService`). |
| **Broker** | A Kafka server. A Kafka cluster has several brokers. |
| **Topic** | A named stream of messages, like a folder or TV channel. Example: `orders`. |
| **Partition** | A slice of a topic. Each partition is an ordered list that only grows at the end. |
| **Offset** | The message number inside one partition (0, 1, 2, 3...). |
| **Consumer group** | A team of consumers that share the work of reading a topic. |
| **Replica** | A copy of a partition on another broker (for safety). |

## 3. What is a "commit log", concretely?
An **append-only, ordered record** of everything written, in the order it happened. Entries are added at the end. Nothing already written is edited or reordered.

```
Log:
  #0  write A
  #1  write B
  #2  write C
  #3  write D   <- next entry goes here
```

Databases use this too (Postgres calls it the **WAL**, SQL Server the **transaction log**): before changing data, the DB first writes "what I am about to do" to a log. This helps recovery after a crash. Kafka's difference: the log **is the product**. You read and write it directly. This is why replay, per-partition ordering and leader-to-follower copying all work the way they do.

### What is inside one record?
| Field | Example | Purpose |
|---|---|---|
| **offset** | `2` | Position in this partition |
| **partition** | `0` | Which log it landed in |
| **key** | `"101"` | Picks the partition. The same key always goes to the same partition. |
| **value** | `{"orderId":101,"status":"PAID"}` | The message body (JSON, Avro, Protobuf) |
| **timestamp** | `2026-09-22T10:15:03Z` | When it was produced |
| **headers** | `{"eventType":"OrderPlacedEvent"}` | Optional metadata |

### Example: topic `order-placed`, Partition 0
```
offset=0  key="101"  value: {"orderId":101,"status":"PLACED","totalPrice":39.98}
offset=1  key="102"  value: {"orderId":102,"status":"PLACED","totalPrice":12.50}
offset=2  key="101"  value: {"orderId":101,"status":"PAID"}
offset=3  key="103"  value: {"orderId":103,"status":"PLACED","totalPrice":99.95}
```
Order `101` appears at offset 0 (placed) and offset 2 (paid). Both are on the **same partition** because the key is the same. So a consumer always sees "placed" before "paid".

## 4. How do partitions affect ordering?
Kafka guarantees order **only inside one partition**, not across the whole topic.

**To keep one order's events in sequence, use the order ID as the message key:**
```java
kafkaTemplate.send("orders", order.getId().toString(), eventJson);
//                  topic     key (orderId)             value
```
Kafka hashes the key to pick the partition. The same key always gives the same partition, and a partition is ordered. So order 123's events (`CREATED`, `PAID`, `SHIPPED`) are read in that order, by one consumer.

- Different orders can go to different partitions and run **in parallel**.
- With **no key**, messages are spread out and order is **not** guaranteed.
- More partitions means more parallelism, but ordering only holds per key.

## 5. How does a consumer group give both scaling and broadcast?
**Rule: inside one group, each partition is read by only one consumer.**

Example, topic with **6 partitions**:

| Consumers in the group | What happens |
|---|---|
| 1 | The one consumer reads all 6. |
| 4 | Split is **2, 2, 1, 1**. |
| 6 | Each reads 1 partition. This is the maximum useful number. |
| 10 | 6 read 1 partition each, **4 sit idle** (standby). If one dies, an idle one takes over. |

To scale beyond the partition count, you need **more partitions**.

**Across groups:** every group gets **its own full copy** of every message and keeps its own offsets. So `inventory-service` and `email-service` can both read the `order-placed` topic, and neither affects the other.

```
                         Producer (sends by key)
                              |
                              v
   +----------------------------------------------------+
   | Topic: order-placed                                 |
   |  [Partition 0]     [Partition 1]     [Partition 2]  |
   +----|-----------------|-----------------|------------+
        v                 v                 v          \
   +---------+       +---------+       +---------+      \  (independent
   |Consumer |       |Consumer |       |Consumer |       \  full copy)
   |A1 (p0)  |       |A2 (p1)  |       |A3 (p2)  |        v
   +---------+       +---------+       +---------+   +-----------+
   Group: inventory-service                          |Consumer B1|
   (1 consumer per partition = parallel scaling)     |(all parts)|
                                                     +-----------+
                                                     Group: email-service
```

## 6. What is an offset, and what does "commit" mean?
The offset is just the **line number inside one partition**. It only counts up and is never reused. Each partition counts on its own, so `(Partition 0, offset 2)` and `(Partition 1, offset 2)` are different messages.

A consumer's bookmark is one number per partition: "I finished up to offset 3". After a restart it asks Kafka for offset 4 onward. Saving that bookmark is called **committing the offset**.

**Process first, then commit:**
```java
@KafkaListener(topics = "orders", groupId = "email-service")
public void listen(String msg, Acknowledgment ack) {
    process(msg);       // 1. do the work
    ack.acknowledge();  // 2. then commit the offset
}
```
- Commit first, then crash while processing: the message is **lost**.
- Process first, then crash before commit: the message comes **again** (a duplicate). A duplicate is safer than a loss, and idempotency (Q7) handles it.

## 7. A consumer crashes after processing but before committing. What happens?
After restart, the consumer reads the **same message again**, because its offset was never saved. So Kafka can deliver a message **twice** (called **at-least-once** delivery).

Your code must be **idempotent**: processing the same message twice must do no harm.

Example: message "charge order 123". The consumer charged the customer, then crashed before the commit. After restart it gets the message again and would charge twice. To prevent this:
- Check the state first: if order 123 is already charged, skip it.
- Or keep a `processed_messages` table with a **unique** message or order ID. The second insert fails, so you skip.

## 8. How does Kafka survive a broker failure? (replicas)
Each partition has **one leader** and some **followers**, on different brokers. Producers and consumers talk to the leader. Followers copy its log. If the leader's broker dies, an up-to-date follower (an **in-sync replica**, ISR) becomes the new leader. No data is lost.

Three settings work together:

| Setting | Where | Meaning |
|---|---|---|
| `replication factor = 3` | Topic | 3 copies of each partition, on 3 brokers |
| `min.insync.replicas = 2` | Topic | At least **2 broker copies** must have the message before the write is accepted |
| `acks=all` | Producer | "Wait until the in-sync copies confirm" |

**Flow with `acks=all`:**
1. The producer sends the message to the leader (Broker 1).
2. The brokers copy it among themselves. The producer is not involved in copying.
3. When at least 2 copies have it (leader plus 1 follower), the producer gets "OK".

The **2 copies are on the brokers**, not on the producer or the consumer.

If fewer than 2 copies are alive, the write is **rejected** (`NotEnoughReplicas`). This is on purpose: it is better to fail loudly than to accept a message that exists in only one place. Without `min.insync.replicas`, `acks=all` could still return "OK" with only the leader holding the message.

Note: the number of replicas cannot be more than the number of brokers. A local setup with 1 broker must use replicas 1 and `min.insync.replicas=1`.

## 9. How do I create a topic with these settings?
Spring creates the topic at startup for every `NewTopic` bean (through `KafkaAdmin`):
```java
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name("orders")       // topic name
            .partitions(8)                       // 8 slices, so up to 8 parallel consumers per group
            .replicas(3)                         // replication factor = 3 copies
            .config("min.insync.replicas", "2")  // need 2 copies before "OK"
            .build();
    }
}
```
- If the topic **already exists**, this bean does **not** change its replicas or `min.insync.replicas`. You must alter the topic with Kafka tools.
- In many companies the Kafka admin or DevOps creates topics, and you only set the producer and consumer properties.

## 10. How do you make sure there is zero message loss?
Protect all three places: **producer, topic/broker, consumer.**

**Producer and consumer: `application.properties`**
```properties
# Producer
spring.kafka.producer.acks=all
spring.kafka.producer.retries=2147483647
spring.kafka.producer.properties.enable.idempotence=true
spring.kafka.producer.properties.delivery.timeout.ms=120000

# Consumer
spring.kafka.consumer.enable-auto-commit=false
spring.kafka.listener.ack-mode=manual
```

**Topic:** replication factor 3 and `min.insync.replicas=2` (Q9).

**Broker:** `unclean.leader.election.enable=false`, so an out-of-date copy can never become leader. This is a broker or topic config, and it is already `false` by default in newer Kafka.

**Consumer side:** commit after processing (Q6), make processing idempotent (Q7), and use a DLQ for messages that keep failing (Q12).

What each part does:
- `acks=all` plus `min.insync.replicas=2`: a message is confirmed only when it is safe on more than one broker.
- `enable.idempotence=true`: producer retries do not create duplicates in the topic.
- Manual commit after processing: a crash causes a re-read, not a loss.

This gives **at-least-once**. Real "exactly-once" needs Kafka transactions or idempotent consumers.

## 11. What is exponential backoff? How do you implement retries?
Wait **longer after each failure**: 1s, 2s, 4s, 8s... Add **jitter** (a small random delay) so many clients do not retry at the same moment.

`delay = min(base * 2^attempt, maxDelay) + random jitter`

```java
// Spring Kafka
DefaultErrorHandler handler = new DefaultErrorHandler(
    new DeadLetterPublishingRecoverer(kafkaTemplate),          // after the last retry -> DLQ
    new ExponentialBackOffWithMaxRetries(5) {{
        setInitialInterval(1000); setMultiplier(2.0); setMaxInterval(30000);
    }});
handler.addNotRetryableExceptions(IllegalArgumentException.class); // don't retry bad data

// Resilience4j / Spring Retry
@Retryable(maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2, random = true))
```

Retry only **temporary** errors (timeout, service down, 503). A permanent error like broken JSON will never succeed, so send it straight to the DLQ.

## 12. What is a DLQ (Dead Letter Queue)?
A DLQ is a **separate topic** (for example `orders.DLQ`) where you park messages that keep failing. Saving them in a DB table is also a valid choice. The idea is the same.

**Problem:** one bad message (say, broken JSON) fails every time. If you retry forever, that partition is stuck, and every message behind it waits.

**Solution:**
1. Retry a few times, with exponential backoff.
2. Still failing? Send the message to the DLQ, together with the error reason.
3. Continue with the next message.
4. Later, a job or a person checks the DLQ, fixes the cause, and re-sends the messages.

Nothing is lost, and nothing blocks the rest.

## 13. The DB transaction commits, but sending to Kafka fails. How do you handle it?
This is the **dual-write problem**. The database and Kafka cannot share one transaction, so one can succeed while the other fails.

**Best fix: the Transactional Outbox pattern.**

The **outbox is a normal table in the same database** (for example `outbox_event`). Steps:
1. In **one DB transaction**, save the business row **and** an outbox row (the event). Both succeed or both roll back.
2. A separate relay job reads `NEW` outbox rows and sends them to Kafka.
3. After Kafka confirms, mark the row `SENT`.

If Kafka is down, rows stay `NEW`, and the relay retries later. Nothing is lost.

```java
@Transactional
public void placeOrder(Order o) {
    orderRepo.save(o);
    outboxRepo.save(new OutboxEvent("OrderCreated", o.getId(), toJson(o)));
}

@Scheduled(fixedDelay = 1000)
public void relay() {
    for (OutboxEvent e : outboxRepo.findTop100ByStatusOrderById("NEW")) {
        kafka.send("orders", e.getAggregateId(), e.getPayload()).get(); // wait for the ack
        e.setStatus("SENT");
        outboxRepo.save(e);
    }
}
```
Result: **at-least-once** delivery, so consumers must be idempotent.

**Weaker fallback:** commit the order, try Kafka, retry a few times, then save the event in a `failed_events` table for a job to resend. It is weaker because that is a **second, separate write**. If the app crashes after the DB commit and before that insert, the event is lost. The outbox avoids this because the event is saved in the same transaction as the order.

## 14. Push or pull?
Kafka consumers **pull** messages. A slow consumer just falls behind (this is called **offset lag**) instead of being flooded. Each consumer controls its own speed and batch size.

## 15. Why is Kafka so fast?
- **Sequential disk writes:** appending to the end of a log is fast, even on spinning disks.
- **Batching:** many messages per network round trip.
- **Zero-copy:** the broker sends log data from the OS cache straight to the network.
- **No per-message DB lookup:** an offset is just a number, and group offsets live in an internal topic called `__consumer_offsets`.

## 16. KRaft vs ZooKeeper?
Old Kafka used a separate **ZooKeeper** service to store cluster metadata (brokers, partition leaders, ACLs). Newer Kafka (3.3+ by default, 4.0+ with no ZooKeeper support at all) uses **KRaft**: the brokers run their own Raft-based consensus. One less service to run, and faster controller failover.

## 17. How would this map onto this project?
Same shape as the RabbitMQ setup, with a different transport:
```java
// Producer (inside OrderService after an order is placed)
@Service
public class OrderEventProducer {
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OrderPlacedEvent event) {
        // key = orderId -> all events of this order stay ordered on one partition
        kafkaTemplate.send("order-placed", event.getOrderId().toString(), event);
    }
}
```
```java
// Consumer (inside InventoryService)
@Component
public class InventoryEventListener {
    @KafkaListener(topics = "order-placed", groupId = "inventory-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        // reserve stock for event.getProductId() / event.getQuantity()
    }
}
```
`OrderService` does not know who is listening. This is the choreography style from `microservices-communication-questions.md` Q19, and it backs a Saga's compensating steps (`spring-boot-expert-questions.md` Q7).

## 18. Kafka vs RabbitMQ: when would you pick which?
| | Kafka | RabbitMQ |
|---|---|---|
| **Model** | Durable log. Consumers pull and track their own offset. | Queue. The broker pushes and tracks ack per message. |
| **Replay** | Yes: reset the offset and re-read. | No: once acked, it is gone (unless re-published). |
| **Ordering** | Per partition only | Per queue (FIFO) |
| **Throughput** | Very high, built for event streams | High, but tuned more for flexible routing |
| **Routing** | Simple (topic and key) | Rich (direct, topic, fanout, headers exchanges) |
| **Best fit** | Event streaming, event sourcing, many independent consumers needing history | Task queues, RPC-style messaging, complex routing, no replay needed |

This project uses RabbitMQ, which fits a simple task-queue need. Kafka would fit better if the system needed several independent consumers replaying the same event history (for example adding analytics later without touching existing consumers).

---

# Practice quiz (with answers)

## Q1. A topic has 6 partitions and a group has 4 consumers. How are partitions shared? What if the group has 10 consumers?
**Answer:** With 4 consumers the split is **2, 2, 1, 1**. With 10 consumers, 6 each read one partition and **4 sit idle** (standby, they take over if one dies). To use more consumers, add more partitions.

## Q2. Two groups, `email` and `analytics`, read the same topic. Does a message go to both groups or only one?
**Answer:** **Both.** Each group keeps its own offsets, so each group reads the full topic at its own pace. Inside one group, though, a message goes to only one consumer.

## Q3. How do you make sure all events of one order are processed in order?
**Answer:** Use the **order ID as the message key.** The same key always goes to the same partition, and a partition keeps order, so one consumer reads that order's events in sequence.

## Q4. A consumer processes a message, then crashes before committing the offset. What happens after restart, and what must your code handle?
**Answer:** It reads the **same message again**. The code must be **idempotent** (handle duplicates), for example by checking whether the order is already processed or by using a unique key in a `processed_messages` table. Also: process first, then commit.

## Q5. A message with broken JSON fails every time. Why is retrying forever a problem, and what do you do instead?
**Answer:** The failing message **blocks the partition**, so everything behind it waits. Retry a few times with exponential backoff, then send the message to a **DLQ** with the error reason, and continue. A job or a person fixes and re-sends from the DLQ later. Broken JSON is a permanent error, so it can go straight to the DLQ without retries.

## Q6. Where does `min.insync.replicas=2` apply: producer, consumer or broker?
**Answer:** **Topic and brokers.** It means 2 broker copies must have the message before the producer gets "OK" (with `acks=all`). The producer only sets `acks=all`, and the consumer is not involved.

## Q7. Your DB commit works but sending to Kafka fails. Best fix?
**Answer:** **Transactional outbox.** Save the business row and an outbox row (a normal table in the same DB) in one transaction. A relay job sends `NEW` rows to Kafka and marks them `SENT`. A `failed_events` table is weaker because it is a second write that can also be lost.
