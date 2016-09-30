/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package net.hydromatic.optiq.tools;

import net.hydromatic.optiq.SchemaPlus;
import net.hydromatic.optiq.jdbc.OptiqConnection;
import net.hydromatic.optiq.jdbc.OptiqSchema;
import net.hydromatic.optiq.prepare.OptiqPrepareImpl;
import net.hydromatic.optiq.prepare.PlannerImpl;
import net.hydromatic.optiq.server.OptiqServerStatement;

import org.eigenbase.relopt.RelOptCluster;
import org.eigenbase.relopt.RelOptSchema;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Tools for invoking Optiq functionality without initializing a container /
 * server first.
 */
public class Frameworks {
  private Frameworks() {
  }


  /**
   * Creates an instance of {@code Planner}
   * @param The configuration used to describe the operation of the planner.
   * @return The Planner object.
   */
  public static Planner getPlanner(FrameworkConfig config) {
    return new PlannerImpl(config);
  }

  /** Piece of code to be run in a context where a planner is available. The
   * planner is accessible from the {@code cluster} parameter, as are several
   * other useful objects. */
  public interface PlannerAction<R> {
    R apply(RelOptCluster cluster, RelOptSchema relOptSchema,
        SchemaPlus rootSchema);
  }

  /** Piece of code to be run in a context where a planner and statement are
   * available. The planner is accessible from the {@code cluster} parameter, as
   * are several other useful objects. The connection and
   * {@link net.hydromatic.optiq.DataContext} are accessible from the
   * statement. */
  public abstract static class PrepareAction<R> {
    private final FrameworkConfig config;

    public PrepareAction() {
      this.config = StdFrameworkConfig.newBuilder() //
          .defaultSchema(Frameworks.createRootSchema()).build();
    }

    public PrepareAction(FrameworkConfig config) {
      this.config = config;
    }

    public FrameworkConfig getConfig() {
      return config;
    }

    public abstract R apply(RelOptCluster cluster, RelOptSchema relOptSchema,
        SchemaPlus rootSchema, OptiqServerStatement statement);
  }

  /**
   * Initializes a container then calls user-specified code with a planner.
   *
   * @param action Callback containing user-specified code
   * @param config FrameworkConfig to use for planner action.
   * @return Return value from action
   */
  public static <R> R withPlanner(final PlannerAction<R> action, //
      FrameworkConfig config) {
    return withPrepare(
        new Frameworks.PrepareAction<R>(config) {
          public R apply(RelOptCluster cluster, RelOptSchema relOptSchema,
              SchemaPlus rootSchema, OptiqServerStatement statement) {
            return action.apply(cluster, relOptSchema, rootSchema);
          }
        });
  }

  /**
   * Initializes a container then calls user-specified code with a planner.
   *
   * @param action Callback containing user-specified code
   * @return Return value from action
   */
  public static <R> R withPlanner(final PlannerAction<R> action) {
    FrameworkConfig config = StdFrameworkConfig.newBuilder() //
        .defaultSchema(Frameworks.createRootSchema()).build();
    return withPlanner(action, config);
  }

  /**
   * Initializes a container then calls user-specified code with a planner
   * and statement.
   *
   * @param action Callback containing user-specified code
   * @return Return value from action
   */
  public static <R> R withPrepare(PrepareAction<R> action) {
    try {
      Class.forName("net.hydromatic.optiq.jdbc.Driver");
      Connection connection =
          DriverManager.getConnection("jdbc:optiq:");
      OptiqConnection optiqConnection =
          connection.unwrap(OptiqConnection.class);
      final OptiqServerStatement statement =
          optiqConnection.createStatement().unwrap(OptiqServerStatement.class);
      //noinspection deprecation
      return new OptiqPrepareImpl().perform(statement, action);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a root schema.
   *
   * @param addMetadataSchema Whether to add "metadata" schema containing
   *    definitions of tables, columns etc.
   */
  public static SchemaPlus createRootSchema(boolean addMetadataSchema) {
    return OptiqSchema.createRootSchema(addMetadataSchema).plus();
  }
}

// End Frameworks.java
