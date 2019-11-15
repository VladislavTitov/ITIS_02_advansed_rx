package ru.vlados.rxexamples

import io.reactivex.Flowable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.Test

class FlowableTest {

    fun getFlowable(f: Flowable<Int>): Flowable<String> = f.observeOn(Schedulers.io()).map { "Hash: $it" }

    @Test
    fun testWithTrampoline() {
        RxJavaPlugins.setIoSchedulerHandler {
            Schedulers.trampoline()
        }

        val f = Flowable.fromIterable(listOf(1, 2, 3, 4, 5))
        val testObserver = getFlowable(f).test()

        testObserver.assertValues(
            "Hash: 1",
            "Hash: 2",
            "Hash: 3",
            "Hash: 4",
            "Hash: 5"
        )
        testObserver.assertComplete()
    }

    // failed caused by asynchronous running of rx chain that doesn't block main thread
    // so test ends before rx chain completes
    @Test
    fun testWithoutTrampoline() {
        val f = Flowable.fromIterable(listOf(1, 2, 3, 4, 5))
        val testObserver = getFlowable(f).test()

        testObserver.assertValues(
            "Hash: 1",
            "Hash: 2",
            "Hash: 3",
            "Hash: 4",
            "Hash: 5"
        )
        testObserver.assertComplete()
    }

}