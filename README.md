Cumulus is a common collection of useful classes for developing server applications.  This library handles common
functions like retry logic, object pooling, and load balancing.

Retry
-----

A retryer can handle retrying code multiple times until a condition has been met

```java
Retryer retryer = Retryers.newRetryer(5);
final AtomicInteger counter = new AtomicInteger();
Integer result = retryer.submitWithRetry(new Callable<Integer>() {
  @Override
  public Integer call() throws Exception {
    if (counter.incrementAndGet() == 5) {
      return counter.get();
    }
    throw new Exception("Not it!");
  }
});
```

The call method of the anonymous class will be recalled until there are no more exceptions.  In this case that is when the function has been executed five times in a row.

Load Balancing
--------------

A load balancer will try to distribute load over all the different elements in a list.

```java
LoadBalancer<String> lb = new RoundRobinLoadBalancer<String>();
List<String> objects = Arrays.asList("A", "B", "C", "D");
...
while(condition) {
  String element = lb.get(objects);
  // do operation
}
```

Load balancing is a very common operation.  If load balancing can be done in hardware, it is better to do that than this interface, but for smaller jobs this will work.

Object Pooling
--------------

When dealing with remote systems you often have resources that are costly to create but can't handle all the load for a system.  When this is the case an Object Pool can make working with this resource simpler.

```java
ExecutingPool<Connection> pool = new PoolBuilder<Connection>()
       .objectFactory(connectionFactory) // defines how to create new pooled entities
       .buildExecutingPool(Retryers.newExponentialBackoffRetryer(10));

pool.execute(new Block<String>() {
  @Override
  public void apply(final Connection connection) {
    connection.append(data);
  }
});
```

There are multiple ways to work with pools but the best one is with an ExecutingPool.  This provides a more functional way of dealing with the resource and handles most edge cases while dealing with pools.


### Miscellaneous

The above is an overview of several of the main things this library provides but is not everything and not everything planned is here yet.

#### Origin of the name

The name Cumulus came from the genus name of a low orbit cloud.  It was refereed to as ["The cloud of choice for 6-yr.-olds"](http://nenes.eas.gatech.edu/Cloud/Clouds.pdf).