/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.misc;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * The Class MemoryUtil.
 */
public class MemoryHelper {
  public static long getProcessMemoryBytes() {
    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    return memoryMXBean.getHeapMemoryUsage().getUsed();
  }

  public static long getProcessMemoryMB() {
    // Performing right shift by 20, is similar to division by 2^20. Prefer using shift operator,
    // since its faster than division.
    return getProcessMemoryBytes() >> 20;
  }

  public static long getProcessMaxMemoryBytes() {
    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    return memoryMXBean.getHeapMemoryUsage().getMax();
  }

  public static long getProcessMaxMemoryMB() {
    // Performing right shift by 20, is similar to division by 2^20. Prefer using shift operator,
    // since its faster than division.
    return getProcessMaxMemoryBytes() >> 20;
  }
}