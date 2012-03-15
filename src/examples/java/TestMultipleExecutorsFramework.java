/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.protobuf.ByteString;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mesos.*;
import org.apache.mesos.Protos.*;


public class TestMultipleExecutorsFramework {
  static class TestScheduler implements Scheduler {
    int launchedTasks = 0;
    int finishedTasks = 0;
    final int totalTasks;

    public TestScheduler() {
      this(5);
    }

    public TestScheduler(int numTasks) {
      totalTasks = numTasks;
    }

    @Override
    public void registered(SchedulerDriver driver, FrameworkID frameworkId) {
      System.out.println("Registered! ID = " + frameworkId.getValue());
    }

    @Override
    public void resourceOffers(SchedulerDriver driver,
                               List<Offer> offers) {
      try {
        File file = new File("./test-executor");

        for (Offer offer : offers) {
          List<TaskDescription> tasks = new ArrayList<TaskDescription>();
          if (!fooLaunched) {
            TaskID taskId = TaskID.newBuilder()
              .setValue("foo")
              .build();

            System.out.println("Launching task " + taskId.getValue());

            TaskDescription task = TaskDescription.newBuilder()
              .setName("task " + taskId.getValue())
              .setTaskId(taskId)
              .setSlaveId(offer.getSlaveId())
              .addResources(Resource.newBuilder()
                            .setName("cpus")
                            .setType(Value.Type.SCALAR)
                            .setScalar(Value.Scalar.newBuilder()
                                       .setValue(1)
                                       .build())
                            .build())
              .addResources(Resource.newBuilder()
                            .setName("mem")
                            .setType(Value.Type.SCALAR)
                            .setScalar(Value.Scalar.newBuilder()
                                       .setValue(128)
                                       .build())
                            .build())
              .setExecutor(ExecutorInfo.newBuilder()
                           .setExecutorId(ExecutorID.newBuilder()
                                          .setValue("executor-foo")
                                          .build())
                           .setCommand(CommandInfo.newBuilder()
                                       .setUri(file.getCanonicalPath())
                                       .setValue(file.getCanonicalPath())
                                       .build())
                           .build())
              .setData(ByteString.copyFromUtf8("100000"))
              .build();

            tasks.add(task);

            fooLaunched = true;
          }

          if (!barLaunched) {
            TaskID taskId = TaskID.newBuilder()
              .setValue("bar")
              .build();

            System.out.println("Launching task " + taskId.getValue());

            TaskDescription task = TaskDescription.newBuilder()
              .setName("task " + taskId.getValue())
              .setTaskId(taskId)
              .setSlaveId(offer.getSlaveId())
              .addResources(Resource.newBuilder()
                            .setName("cpus")
                            .setType(Value.Type.SCALAR)
                            .setScalar(Value.Scalar.newBuilder()
                                       .setValue(1)
                                       .build())
                            .build())
              .addResources(Resource.newBuilder()
                            .setName("mem")
                            .setType(Value.Type.SCALAR)
                            .setScalar(Value.Scalar.newBuilder()
                                       .setValue(128)
                                       .build())
                            .build())
              .setExecutor(ExecutorInfo.newBuilder()
                           .setExecutorId(ExecutorID.newBuilder()
                                          .setValue("executor-bar")
                                          .build())
                           .setCommand(CommandInfo.newBuilder()
                                       .setUri(file.getCanonicalPath())
                                       .setValue(file.getCanonicalPath())
                                       .build())
                           .build())
              .setData(ByteString.copyFrom("100000".getBytes()))
              .build();

            tasks.add(task);

            barLaunched = true;
          }

          driver.launchTasks(offer.getId(), tasks);
        }
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }

    @Override
    public void offerRescinded(SchedulerDriver driver, OfferID offerId) {}

    @Override
    public void statusUpdate(SchedulerDriver driver, TaskStatus status) {
      System.out.println("Status update: task " + status.getTaskId() +
                         " is in state " + status.getState());
      if (status.getState() == TaskState.TASK_FINISHED) {
        finishedTasks++;
        System.out.println("Finished tasks: " + finishedTasks);
        if (finishedTasks == totalTasks)
          driver.stop();
      }
    }

    @Override
    public void frameworkMessage(SchedulerDriver driver, SlaveID slaveId, ExecutorID executorId, byte[] data) {}

    @Override
    public void slaveLost(SchedulerDriver driver, SlaveID slaveId) {}

    @Override
    public void error(SchedulerDriver driver, int code, String message) {
      System.out.println("Error: " + message);
    }

    private boolean fooLaunched = false;
    private boolean barLaunched = false;
  }

  private static void usage() {
    String name = TestMultipleExecutorsFramework.class.getName();
    System.err.println("Usage: " + name + " master <tasks>");
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 1 || args.length > 2) {
      usage();
      System.exit(1);
    }

    String uri = new File("./test-executor").getCanonicalPath();

    ExecutorInfo executorInfo = ExecutorInfo.newBuilder()
      .setExecutorId(ExecutorID.newBuilder().setValue("default").build())
      .setCommand(CommandInfo.newBuilder().setUri(uri).setValue(uri).build())
      .build();

    MesosSchedulerDriver driver;

    if (args.length == 1) {
      driver = new MesosSchedulerDriver(
          new TestScheduler(),
          "Java test framework",
          executorInfo,
          args[0]);
    } else {
      driver = new MesosSchedulerDriver(
          new TestScheduler(Integer.parseInt(args[1])),
          "Java test framework",
          executorInfo,
          args[0]);
    }

    System.exit(driver.run() == Status.DRIVER_STOPPED ? 0 : 1);
  }
}
