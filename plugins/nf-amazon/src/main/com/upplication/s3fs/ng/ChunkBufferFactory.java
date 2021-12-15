/*
 * Copyright 2020-2021, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.upplication.s3fs.ng;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
public class ChunkBufferFactory {

    final Logger log = LoggerFactory.getLogger(ChunkBufferFactory.class);

    final private BlockingQueue<ChunkBuffer> pool;

    final private AtomicInteger count;

    private final int chunkSize;

    private final int capacity;

    public ChunkBufferFactory(int chunkSize, int capacity) {
        this.chunkSize = chunkSize;
        this.capacity = capacity;
        this.pool = new LinkedBlockingQueue<>();
        this.count = new AtomicInteger();
    }

    private double logistic(float x, double A, double K) {
        return A / ( 1 + Math.exp( -1 * K * x ) );
    }

    private int delay( int current, int capacity ) {
        float x = (float)current / capacity * 100;
        return (int)Math.round(logistic(x-90, 100_000, 0.5));
    }

    public ChunkBuffer create(int index) throws InterruptedException {
        ChunkBuffer result = pool.poll(100, TimeUnit.MILLISECONDS);
        if( result != null ) {
            result.clear();
            return result.withIndex(index);
        }

        // add logistic delay to slow down the allocation of new buffer
        // when the request approach or exceed the max capacity
        final int cc = count.getAndIncrement();
        final int dd = delay(cc, capacity);
        log.debug("Creating a new buffer count={}; capacity={}; delay={}", cc, capacity, dd);
        Thread.sleep( dd );
        return new ChunkBuffer(this,chunkSize,index);

    }

    void giveBack(ChunkBuffer buffer) {
        if( pool.size()<capacity ) {
            pool.add(buffer);
            log.debug("Returning buffer index={} to pool size={}", buffer.getIndex(), pool.size());
        }
        else {
            int cc = count.decrementAndGet();
            log.debug("Returning buffer index={} for GC; pool size={}; count={}", buffer.getIndex(), pool.size(), cc);
        }
    }

    int getPoolSize() { return pool.size(); }
}