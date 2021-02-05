// Copyright 2018 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.blackbox.framework;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * Abstract class for setting up the blackbox test environment, returns {@link BlackBoxTestContext}
 * for the tests to access the environment and run Bazel/Blaze commands. This is the single entry
 * point in the blackbox tests framework for working with test environment.
 *
 * <p>The single instance of this class can be used for several tests. Each test should call {@link
 * #prepareEnvironment(String, ImmutableList)} to prepare environment and obtain the test context.
 *
 * <p>See {@link com.google.devtools.build.lib.blackbox.junit.AbstractBlackBoxTest}
 */
public abstract class BlackBoxTestEnvironment {
  /**
   * Executor service for reading stdout and stderr streams of the process. Has exactly two threads
   * since there are two streams.
   */
  @Nullable
  private ExecutorService executorService =
      MoreExecutors.getExitingExecutorService(
          (ThreadPoolExecutor) Executors.newFixedThreadPool(2),
          1, TimeUnit.SECONDS);

  protected abstract BlackBoxTestContext prepareEnvironment(
      String testName, ImmutableList<ToolsSetup> tools, ExecutorService executorService)
      throws Exception;

  /**
   * Prepares test environment and returns test context instance to be used by tests to access the
   * environment and invoke Bazel/Blaze commands.
   *
   * @param testName name of the current test, used to name the test working directory
   * @param tools the list of all tools setup classes {@link ToolsSetup} that should be called for
   *     the test
   * @return {@link BlackBoxTestContext} test context
   * @throws Exception if tools setup fails
   */
  public BlackBoxTestContext prepareEnvironment(String testName, ImmutableList<ToolsSetup> tools)
      throws Exception {
    Preconditions.checkNotNull(executorService);
    return prepareEnvironment(testName, tools, executorService);
  }

  /**
   * This method must be called when the test group execution is finished, for example, from
   * &#64;AfterClass method.
   */
  public final void dispose() {
    Preconditions.checkNotNull(executorService);
    MoreExecutors.shutdownAndAwaitTermination(executorService, 1, TimeUnit.SECONDS);
    executorService = null;
  }

  public static String getWorkspaceWithDefaultRepos() {
    return Joiner.on("\n")
        .join(
            "load('@bazel_tools//tools/build_defs/repo:http.bzl', 'http_archive')",
            "http_archive(",
            "    name = 'rules_cc',",
            "    sha256 = '812a3924348af40492017e7ca6f44819f572dae57bd4c736d2853b4f03522c45',",
            "    strip_prefix = 'rules_cc-2174aa631a0c32cb14ca0782af43aa0bd0aa1bb3',",
            "    urls = [",
            "        'https://github.com/wrowe/rules_cc/archive/"
                + "2174aa631a0c32cb14ca0782af43aa0bd0aa1bb3.zip',",
            "    ],",
            ")",
            "http_archive(",
            "    name = 'rules_proto',",
            "    sha256 = '8e7d59a5b12b233be5652e3d29f42fba01c7cbab09f6b3a8d0a57ed6d1e9a0da',",
            "    strip_prefix = 'rules_proto-7e4afce6fe62dbff0a4a03450143146f9f2d7488',",
            "    urls = [",
            "        'https://mirror.bazel.build/github.com/bazelbuild/rules_proto/archive/"
                + "7e4afce6fe62dbff0a4a03450143146f9f2d7488.tar.gz',",
            "        'https://github.com/bazelbuild/rules_proto/archive/"
                + "7e4afce6fe62dbff0a4a03450143146f9f2d7488.tar.gz',",
            "    ],",
            ")");
  }
}
