/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.swiftmq.ms.artemis.util;


public class ByteUtil {

   public static boolean equals(final byte[] left, final byte[] right) {
      return equals(left, right, 0, right.length);
   }

   public static boolean equals(final byte[] left,
                                final byte[] right,
                                final int rightOffset,
                                final int rightLength) {
      if (left == right)
         return true;
      if (left == null || right == null)
         return false;
      if (left.length != rightLength)
         return false;
         return equalsSafe(left, right, rightOffset, rightLength);
   }

   private static boolean equalsSafe(byte[] left, byte[] right, int rightOffset, int rightLength) {
      for (int i = 0; i < rightLength; i++)
         if (left[i] != right[rightOffset + i])
            return false;
      return true;
   }

}
