/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.CoordinatorActionInfo;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.XException;
import org.apache.oozie.command.PreconditionException;
import org.apache.oozie.executor.jpa.CoordJobGetActionsForDatesJPAExecutor;
import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.service.JPAService;
import org.apache.oozie.service.Services;
import org.apache.oozie.util.DateUtils;
import org.apache.oozie.util.ParamChecker;
/**
 * This class provides the utility of listing
 * coordinator actions that were executed between a certain
 * date range. This is helpful in turn for retrieving the
 * required logs in that date range.
 */
public class CoordActionsInDateRange {

    /**
     * Get the list of actions for given date ranges
     *
     * @param jobId coordinator job id
     * @param scope the date range for log. format is comma-separated list of date ranges. Each date range element is specified with two dates separated by '::'
     * @return the list of coordinator actions for the date range
     *
     * Internally involves a database operation by invoking method 'getActionIdsFromDateRange'.
     */
    public static List<CoordinatorActionBean> getCoordActionsFromDates(String jobId, String scope) throws XException {
        ParamChecker.notEmpty(jobId, "jobId");
        ParamChecker.notEmpty(scope, "scope");
        Set<CoordinatorActionBean> actionSet = new HashSet<CoordinatorActionBean>();
        String[] list = scope.split(",");
        for (String s : list) {
            s = s.trim();
            if (s.contains("::")) {
                List<CoordinatorActionBean> listOfActions = getCoordActionsFromDateRange(jobId, s);
                actionSet.addAll(listOfActions);
            }
            else {
                throw new XException(ErrorCode.E0308, "'" + s + "'. Separator '::' is missing for start and end dates of range");
            }
        }
        List<CoordinatorActionBean> coordActions = new ArrayList<CoordinatorActionBean>();
        for (CoordinatorActionBean coordAction : actionSet) {
            coordActions.add(coordAction);
        }
        return coordActions;
    }

    /**
     * Get the coordinator actions for a given date range
     * @param jobId the coordinator job id
     * @param range the date range separated by '::'
     * @return the list of Coordinator actions for the date range
     * @throws XException
     */
    public static List<CoordinatorActionBean> getCoordActionsFromDateRange(String jobId, String range) throws XException{
            String[] dateRange = range.split("::");
            // This block checks for errors in the format of specifying date range
            if (dateRange.length != 2) {
                throw new XException(ErrorCode.E0308, "'" + range + "'. Date value expected on both sides of the scope resolution operator '::' to signify start and end of range");
            }
            Date start;
            Date end;
            try {
            // Get the start and end dates for the range
                start = DateUtils.parseDateUTC(dateRange[0].trim());
                end = DateUtils.parseDateUTC(dateRange[1].trim());
            }
            catch (ParseException dx) {
                throw new XException(ErrorCode.E0308, "Error in parsing start or end date. " + dx);
            }
            if (start.after(end)) {
                throw new XException(ErrorCode.E0308, "'" + range + "'. Start date '" + start + "' is older than end date: '" + end + "'");
            }
            List<CoordinatorActionBean> listOfActions = getActionIdsFromDateRange(jobId, start, end);
            return listOfActions;
    }

    /*
     * Get coordinator action ids between given start and end time
     *
     * @param jobId coordinator job id
     * @param start start time
     * @param end end time
     * @return a list of coordinator actions that correspond to the date range
     */
    private static List<CoordinatorActionBean> getActionIdsFromDateRange(String jobId, Date start, Date end) throws XException{
        List<CoordinatorActionBean> list;
        JPAService jpaService = Services.get().get(JPAService.class);
        list = jpaService.execute(new CoordJobGetActionsForDatesJPAExecutor(jobId, start, end));
        return list;
    }
}
