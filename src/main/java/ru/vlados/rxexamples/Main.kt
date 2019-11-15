package ru.vlados.rxexamples

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.pow

fun main() {
    mapListAndLogExample()

    retryWhenExample()
    TimeUnit.SECONDS.sleep(10)
}

fun mapListAndLogExample() {
    val source = Flowable.fromIterable(
        listOf(
            listOf(1, 2, 3, 4, 5),
            listOf(6, 7, 8, 9, 10),
            listOf(11, 12, 13)
        )
    )
    source.mapAndLog().subscribe()
    source.compose(MapListAndLog()).subscribe()

}

fun Flowable<List<Int>>.mapAndLog(): Flowable<List<String>> {
    return this
        .flatMap {
            Flowable
                .fromIterable(it)
                .map {
                    "$it!"
                }
                .toList()
                .toFlowable()
        }
        .doOnNext {
            println(it)
        }
}

class MapListAndLog : FlowableTransformer<List<Int>, List<String>> {
    override fun apply(upstream: Flowable<List<Int>>): Publisher<List<String>> {
        return upstream
            .flatMap {
                Flowable
                    .fromIterable(it)
                    .map {
                        "$it!"
                    }
                    .toList()
                    .toFlowable()
            }
            .doOnNext {
                println(it)
            }
    }
}

fun retryWhenExample() {
    // create flowable with error after 4 pushed items
    Flowable.create<Int>({ emitter ->
        for (i in 1..4) {
            if (!emitter.isCancelled) {
                emitter.onNext(i)
            }
        }
        emitter.onError(TimeoutException())
    }, BackpressureStrategy.BUFFER)
        // Uncomment it if you need for resubscribing every time with some delay or extend it
        // .repeatWhen {
        //     it.delay(100L, TimeUnit.MILLISECONDS)
        // }
        .retryWhen {
            Flowable.zip<Throwable, Int, Int>(
                it,
                // retry for 2 times
                Flowable.range(1, 2), // can be replaced with Flowable.range(1, Int.MAX_VALUE) or anything else
                BiFunction { i1, i2 ->
                    // if exception is TimeoutException, go further and resubscribe
                    // else throw error to stop chain running
                    if (i1 is TimeoutException) {
                        i2
                    } else {
                        throw i1
                    }
                }
            ).flatMap {
                // set exponential-grown delay
                val twoDegree = 2.0.pow(it)
                Flowable.just(it)
                    .delay(twoDegree.toLong(), TimeUnit.SECONDS)
            }
        }
        .subscribeOn(Schedulers.single())
        .subscribe({
            println(it)
        }, {
            println(it.message)
        }, {
            println("Completed")
        })
}
