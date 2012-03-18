package org.openmole.plugin.environment.jsaga;


import org.ogf.saga.job.JobDescription;

/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 *
 * @author reuillon
 */
public class Requirement {
  public static final String MEMORY = JobDescription.TOTALPHYSICALMEMORY;
  public static final String CPU_TIME = JobDescription.TOTALCPUTIME;
  public static final String CPU_COUNT = JobDescription.TOTALCPUCOUNT;
  public static final String CPU_ARCHITECTURE = JobDescription.CPUARCHITECTURE;
}
