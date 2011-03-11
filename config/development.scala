import com.twitter.haplocheirus._
import com.twitter.gizzard.config._
import com.twitter.querulous.config._
import com.twitter.querulous.query.QueryClass
import com.twitter.util.TimeConversions._


new HaplocheirusConfig {
  val adminConfig = new AdminConfig {
    val httpPort = 7667
    val textPort = 7668
    val timeSeries = true
  }

  val server = new HaplocheirusServer with THsHaServer {
    timeout = 15.seconds
    idleTimeout = 300.seconds
    threadPool.minThreads = 10
  }

  jobInjector.timeout = 15.seconds
  jobInjector.idleTimeout = 300.seconds
  jobInjector.threadPool.minThreads = 1

  val nameServer = new NameServer {
    mappingFunction = ByteSwapper
    val replicas = Seq(Memory)
  }

  val redisConfig = new RedisConfig {
    val readPoolConfig = new RedisPoolConfig {
      val poolSize = 1
      val poolTimeoutMsec = 5000
      val pipeline = 100
      val timeoutMsec = 200
      val keysTimeoutMsec = 5000
      val expirationHours = 24 * 21
      val autoDisableErrorLimit = 200
      val autoDisableDuration = 60.seconds
    }

    val writePoolConfig = new RedisPoolConfig {
      val poolSize = 1
      val poolTimeoutMsec = 5000
      val pipeline = 100
      val timeoutMsec = 200
      val keysTimeoutMsec = 5000
      val expirationHours = 24 * 21
      val autoDisableErrorLimit = 200
      val autoDisableDuration = 60.seconds
    }

    val rangeQueryPageSize = 20
  }

  val timelineTrimConfig = new TimelineTrimConfig {
    val bounds = Map(
      "default" -> new TimelineTrimBounds {
        val lower = 800
        val upper = 850
      }
    )
  }

  val replicationFuture = new Future {
    poolSize = 100
    maxPoolSize = 100
    keepAlive = 5.seconds
    timeout = 6.seconds
  }

  val readFuture = new Future {
    poolSize = 100
    maxPoolSize = 100
    keepAlive = 5.seconds
    timeout = 6.seconds
  }

  val jobQueues = Map(
    Priority.Copy.id -> new Scheduler {
      val name = "copy"
      val schedulerType = new KestrelScheduler {
        path = "/tmp"
        keepJournal = false
      }
      threads = 1
      errorLimit = 25
      errorRetryDelay = 900.seconds
      badJobQueue = new JsonJobLogger { name = "bad_jobs" }
    },

    Priority.Write.id -> new Scheduler {
      val name = "write"
      val schedulerType = new KestrelScheduler {
        path = "/tmp"
        keepJournal = false
      }
      threads = 1
      errorLimit = 25
      errorRetryDelay = 60.seconds
      badJobQueue = new JsonJobLogger { name = "bad_jobs" }
    },

    Priority.MultiPush.id -> new Scheduler {
      val name = "multipush"
      val schedulerType = new MemoryScheduler
      threads = 1
      errorLimit = 25
      errorRetryDelay = 60.seconds
      badJobQueue = new JsonJobLogger { name = "bad_jobs" }
    }
  )

  logging = new LogConfigString("""
                                level = "info"
                                console = false
                                filename = "haplocheirus.log"
                                roll = "never"
                                throttle_period_msec = 60000
                                throttle_rate = 10

                                stats {
                                  node = "stats"
                                use_parents = false
                                level = "info"
                                filename = "stats.log"
                                }
                                """)
}