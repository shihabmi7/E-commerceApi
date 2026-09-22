# Kafka — Beginner to Mid-Level Interview Questions

General-purpose Kafka notes (this repo currently uses **RabbitMQ**, not Kafka — see `spring.rabbitmq.*` in `application.properties` — so nothing here is wired into the codebase). Pairs with `microservices-communication-questions.md` Q4/Q14 (async, event-driven communication) and Q19 (choreography).

## 1. What is Kafka, in short?
A distributed, durable **commit log**. Producers append messages to it; consumers read them at their own pace, independently of each other. Unlike a traditional message queue, messages aren't removed once read — they're retained for a configured period regardless of consumption, which is what makes replay possible.

## 2. What is a "commit log", concretely?
An **append-only, ordered record of every write, kept in the exact sequence it happened.** Every entry is added at the end and gets a sequence number (in Kafka, that's the offset — see Q5); nothing already written is ever edited or reordered in place:
```
Log:
  #0  write A
  #1  write B
  #2  write C
  #3  write D   <- next entry goes here
```
Databases use this internally too — Postgres/MySQL call it a **write-ahead log (WAL)**, SQL Server a **transaction log**: before changing any actual data, the DB first appends "here's what I'm about to do" as a new log entry, which is what lets it recover after a crash (replay the log) and lets replicas stay in sync (replay the same log elsewhere). Kafka's difference is that it **exposes this log directly as the product** — instead of a database hiding it as an internal detail, in Kafka the log itself *is* the thing you read and write (a partition, see Q3). That framing is also why replay (Q1), strict per-partition ordering (Q4), and leader→follower replication (Q7) all work the way they do — they all fall straight out of "replay/copy the log, in order."

### Anatomy of one record

Each entry Kafka appends isn't just a raw string — it's a structured record:

| Field | Example | Purpose |
|---|---|---|
| **offset** | `2` | Position in this partition's log (see Q5) |
| **partition** | `0` | Which log this landed in |
| **key** | `"101"` | Decides the partition — same key always lands on the same partition, keeping that key's messages in order |
| **value** (payload) | `{"orderId":101,"status":"PAID",...}` | The actual message body — usually JSON, Avro, or Protobuf |
| **timestamp** | `2026-09-22T10:15:03Z` | When it was produced (or appended, depending on config) |
| **headers** | `{"eventType":"OrderPlacedEvent"}` | Optional metadata, like HTTP headers |

### Sample commit log with real payloads — `order-placed`, Partition 0

```
offset=0  key="101"  timestamp=2026-09-22T10:14:50Z
  headers: { "eventType": "OrderPlacedEvent" }
  value: {"orderId":101,"userId":55,"productId":9,"quantity":2,"status":"PLACED","totalPrice":39.98}

offset=1  key="102"  timestamp=2026-09-22T10:14:57Z
  headers: { "eventType": "OrderPlacedEvent" }
  value: {"orderId":102,"userId":61,"productId":14,"quantity":1,"status":"PLACED","totalPrice":12.50}

offset=2  key="101"  timestamp=2026-09-22T10:15:03Z
  headers: { "eventType": "OrderStatusChangedEvent" }
  value: {"orderId":101,"status":"PAID","paidAt":"2026-09-22T10:15:03Z"}

offset=3  key="103"  timestamp=2026-09-22T10:15:10Z
  headers: { "eventType": "OrderPlacedEvent" }
  value: {"orderId":103,"userId":55,"productId":2,"quantity":5,"status":"PLACED","totalPrice":99.95}
```

Notice `orderId 101` appears twice — offset `0` (placed) and offset `2` (paid) — both on **the same partition** because they share the same key (`"101"`). That's exactly what guarantees a consumer sees "placed" before "paid," never the reverse.

From the CLI, this is what it looks like to actually read that log:
```bash
kafka-console-consumer.sh --topic order-placed --partition 0 \
  --from-beginning --property print.key=true --property print.offset=true
```
```
Offset:0 101 {"orderId":101,"userId":55,"productId":9,"quantity":2,"status":"PLACED","totalPrice":39.98}
Offset:1 102 {"orderId":102,"userId":61,"productId":14,"quantity":1,"status":"PLACED","totalPrice":12.50}
Offset:2 101 {"orderId":101,"status":"PAID","paidAt":"2026-09-22T10:15:03Z"}
Offset:3 103 {"orderId":103,"userId":55,"productId":2,"quantity":5,"status":"PLACED","totalPrice":99.95}
```

## 3. What are the core building blocks?
| Concept | What it is |
|---|---|
| **Broker** | A Kafka server that stores data and serves clients. A cluster has several. |
| **Topic** | A named stream of messages, e.g. `order-placed`. |
| **Partition** | A topic is split into partitions — ordered, append-only logs, each message getting an increasing **offset**. |
| **Producer** | Writes messages to a topic. The message key decides the partition. |
| **Consumer** | Reads messages, tracking its position via offset. |
| **Consumer group** | A set of consumers sharing a topic's work — each partition is read by only one consumer *within* a group; different groups each get every message independently. |

## 4. How does partitioning affect ordering?
Kafka only guarantees order **within a single partition**, not across a whole topic. A producer sends messages with the same key (e.g. `orderId`) to the same partition every time (via the key's hash), which keeps that key's messages in order. Messages for different keys can land on different partitions and interleave with no ordering guarantee between them. More partitions means more parallelism (more consumers can read concurrently), so partition count is a direct trade-off between throughput and ordering guarantees.

## 5. How does a consumer group provide both scaling and broadcast?
- **Within a group:** each partition is assigned to exactly one consumer, so adding consumers (up to the partition count) parallelizes processing — this is how you scale reads.
- **Across groups:** every group gets its own copy of every message, independently. So `inventory-service` (group A) and `email-service` (group B) can both consume the same `order-placed` topic, each tracking its own offsets, with neither affecting the other.

If a group has more consumers than partitions, the extra consumers sit idle — you can't split one partition's messages across two consumers in the same group.

```
                         Producer
                        (sends by key)
                             |
                             v
   +----------------------------------------------------+
   | Topic: order-placed                                 |
   |  [Partition 0]     [Partition 1]     [Partition 2]  |
   +----|-----------------|-----------------|-------------+
        |                 |                 |         \
        v                 v                 v          \  (independent
   +---------+       +---------+       +---------+       \  full copy)
   |Consumer |       |Consumer |       |Consumer |         \
   |A1 (p0)  |       |A2 (p1)  |       |A3 (p2)  |          v
   +---------+       +---------+       +---------+     +-----------+
   Consumer group:                                      |Consumer B1|
   inventory-service                                     |(all parts)|
   (1 consumer per partition = parallel scaling)          +-----------+
                                                      Consumer group:
                                                      email-service
                                                      (own full copy)
```

Each partition is an **append-only log** — a file you can only ever write to at the *end*, never edit or delete in place:
```
Partition 0:  [0] orderId 101 PLACED
              [1] orderId 102 PLACED
              [2] orderId 101 PAID
              [3] orderId 103 PLACED   <- next write lands here, offset 4
```
That's why it's fast (sequential disk writes, no row updates) and why replay works (old offsets are never overwritten — they just age out after the retention period).

**What "increasing offset" actually means:** the offset is nothing more than the line number of a message *within its own partition* — like an array index. The very first message written to `Partition 0` gets offset `0`. The next one gets `1`. Then `2`, `3`, `4`... it only ever counts up, one at a time, in the exact order Kafka received them:

```
Partition 0 (only this partition's own counter):
  offset 0 -> orderId 101 PLACED
  offset 1 -> orderId 102 PLACED
  offset 2 -> orderId 101 PAID       <- 3rd message written, so offset = 2
  offset 3 -> orderId 103 PLACED     <- 4th message written, so offset = 3
```

Two things trip people up:
1. **It never goes backward or gets reused.** Nothing is ever inserted at offset `1.5` and nothing is ever deleted and renumbered — a message keeps its offset forever, even after other consumers have read past it or the message itself has aged out of retention.
2. **Each partition counts on its own.** `Partition 0`'s offsets (`0,1,2,3...`) have nothing to do with `Partition 1`'s offsets (which *also* start at `0,1,2,3...`). "Offset 2" only means something once you say *which partition* — `(Partition 0, offset 2)` and `(Partition 1, offset 2)` are two completely different messages.

**Why it matters:** a consumer's "position" in a partition is just one number — the last offset it has processed. To resume after a restart, it doesn't need to remember message contents, just `"I'm at offset 3 in Partition 0"` — then it asks Kafka for offset `4` onward. That single incrementing number is the entire bookmark.

The **producer** isn't a separate Kafka component you install — it's just whichever part of your own application code publishes the message. In this project, if `Cart`/`Order` flows used Kafka, `OrderService` would *be* the producer: the moment an order is placed, its code calls `kafkaTemplate.send("order-placed", orderId, event)`. It doesn't need to know who (if anyone) is consuming it.

## 6. Producer vs. consumer — push or pull?
Kafka consumers **pull** messages, rather than the broker pushing them. This means a slow consumer just falls behind (offset lag) instead of being overwhelmed by an unstoppable stream, and it lets each consumer control its own read rate/batch size.

## 7. How does Kafka survive a broker failure?
Each partition has one **leader** replica and N **follower** replicas, spread across different brokers. Producers and consumers only talk to the leader; followers replicate its log. If the leader's broker dies, a follower (one that was caught up — an "in-sync replica") is elected the new leader. `acks=all` on the producer waits for the message to be replicated to all in-sync replicas before confirming the write, trading a little latency for durability against a leader failure right after the write.

## 8. What does "Kafka is stateless/fast" actually rely on?
- **Sequential disk writes:** appending to the end of a log is fast even on spinning disks; Kafka avoids random-access I/O.
- **Batching:** producers and consumers batch multiple messages per network round trip.
- **Zero-copy transfer:** the broker can send log segments straight from the page cache to the network socket without extra copies through user space.
- **No per-message DB lookup:** offsets are just an integer position in the log, and consumer group coordination is handled by the brokers themselves (via an internal `__consumer_offsets` topic), not an external datastore.

## 9. KRaft vs. ZooKeeper?
Older Kafka versions used **ZooKeeper** as a separate service to store cluster metadata (broker list, partition leadership, ACLs). Newer versions (Kafka 3.3+ as the default, 4.0+ dropping ZooKeeper support entirely) use **KRaft**, where the brokers themselves run a Raft-based consensus protocol to manage metadata — one less moving part to operate and a faster controller failover.

## 10. How would this map onto this project (e-commerce, currently RabbitMQ)?
Same shape as the RabbitMQ setup, different transport:
```java
// Producer (e.g. inside OrderService after an order is placed)
@Service
public class OrderEventProducer {
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OrderPlacedEvent event) {
        // key = orderId -> all events for this order stay ordered on one partition
        kafkaTemplate.send("order-placed", event.getOrderId().toString(), event);
    }
}
```
```java
// Consumer (e.g. inside InventoryService)
@Component
public class InventoryEventListener {
    @KafkaListener(topics = "order-placed", groupId = "inventory-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        // reserve stock for event.getProductId() / event.getQuantity()
    }
}
```
`OrderService` doesn't need to know who's listening (`InventoryService`, an email service, analytics, etc.) — this is the choreography style from `microservices-communication-questions.md` Q19, and it's what backs a Saga's compensating-transaction chain (Q-saga in that same family of notes) when a downstream step fails.

## 11. Kafka vs. RabbitMQ — when would you pick which?
| | Kafka | RabbitMQ |
|---|---|---|
| **Model** | Durable log, consumers pull and track their own offset | Traditional queue, broker pushes and tracks delivery/ack per message |
| **Replay** | Yes — reset a consumer's offset to re-read old messages | No — once acked/consumed, it's gone (unless re-published) |
| **Ordering** | Per-partition only | Per-queue (FIFO), simpler to reason about for a single queue |
| **Throughput** | Very high, built for large event streams / stream processing | High, but optimized more for flexible routing than raw log throughput |
| **Routing** | Simple (topic name, sometimes topic + key) | Rich (exchanges: direct/topic/fanout/headers) |
| **Best fit** | Event streaming, event sourcing, high-volume pipelines, multiple independent consumers needing the full history | Task queues, RPC-style messaging, complex routing, when you don't need replay |

This project uses RabbitMQ, which fits a simpler task-queue/routing need; Kafka would be the better fit if the system grew into needing multiple independent consumers replaying the same event history (e.g. adding an analytics pipeline later without touching existing consumers).
