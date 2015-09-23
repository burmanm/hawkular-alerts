/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.alerts.api.model.condition;

import org.hawkular.alerts.api.model.event.Event;
import org.hawkular.alerts.api.model.trigger.Mode;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * An <code>EventCondition</code> is used for condition evaluations over Event data.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class EventCondition extends Condition {

    @JsonInclude(Include.NON_NULL)
    private String expression;

    public EventCondition() {
        this("DefaultId", Mode.FIRING, 1, 1, null);
    }

    public EventCondition(String triggerId, String expression) {
        this(triggerId, Mode.FIRING, 1, 1, expression);
    }

    public EventCondition(String triggerId, Mode triggerMode, String expression) {
        this(triggerId, triggerMode, 1, 1, expression);
    }

    public EventCondition(String triggerId, int conditionSetSize, int conditionSetIndex, String expression) {
        this(triggerId, Mode.FIRING, conditionSetSize, conditionSetIndex, expression);
    }

    public EventCondition(String triggerId, Mode triggerMode, int conditionSetSize, int conditionSetIndex,
                          String expression) {
        super(triggerId, triggerMode, conditionSetSize, conditionSetIndex, Type.EVENT);
        this.expression = expression;
    }

    public String getLog(Event value) {
        return triggerId + " : " + value + " " + expression;
    }

    @Override
    public String getDataId() {
        return null;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public boolean match(Event value) {
        if (null == expression || expression.isEmpty() || null == value) {
            return false;
        }
        String[] expressions = expression.split(",");
        for (int i = 0; i < expressions.length; i++) {
            if (!processExpression(expressions[i], value)) {
                return false;
            }
        }
        return true;
    }

    private static final String TENANT_ID = "tenantId";
    private static final String ID = "id";
    private static final String CTIME = "ctime";
    private static final String EVENT_TEXT = "eventText";
    private static final String CONTEXT = "context.";
    private static final String TAGS = "tags.";

    private static final String EQ = "==";
    private static final String NON_EQ = "!=";
    private static final String STARTS = "starts";
    private static final String ENDS = "ends";
    private static final String CONTAINS = "contains";
    private static final String MATCHES = "matches";
    private static final String LT = "<";
    private static final String LTE = "<=";
    private static final String GT = ">";
    private static final String GTE = ">=";

    /**
     * Expression has awlays following 3 tokens structure:
     *
     * <event.field> <operator> <constant>
     *
     * - <event.field> represent a fixed field of event structure or a key of tags/context
     *   i.e. event.triggerId, event.tenantId, event.context.category, event.tag.server, etc.
     *
     * - <operator> is a string representing a string/numeric operator, supported ones are:
     *   "==" equals
     *   "!=" not equals
     *   "starts" starts with String operator
     *   "ends" ends with String operator
     *   "contains" contains String operator
     *   "match" match String operator
     *   "<" less than
     *   "<=" less or equals than
     *   ">" greater than
     *   ">=" greater or equals than
     *   "==" equals
     *
     * - <constant> is a string that might be interpreted as a number if is not closed with single quotes or a string
     * constant if it is closed with single quotes
     * i.e. 23, 'test'
     *
     * So, putting everything together, a valid expression might look like:
     * event.id start 'IDXYZ', event.context.category == 'Server', event.tag.from end '.com'
     *
     * A non valid expression will return false.
     *
     * @param expression Expression to match with an event
     * @param value Event to match
     * @return result of expression match
     */
    private boolean processExpression(String expression, Event value) {
        if (null == expression || expression.isEmpty() || null == value) {
            return false;
        }
        String[] tokens = expression.split(" ");
        if (tokens.length != 3) {
            return false;
        }
        String eventField = tokens[0];
        String sEventValue = null;
        Long lEventValue = null;
        String operator = tokens[1];
        String constant = tokens[2];
        String sConstantValue = null;
        Double dConstantValue = null;

        if (eventField == null || eventField.isEmpty()) {
            return false;
        }
        if (TENANT_ID.equals(eventField)) {
            sEventValue = value.getTenantId();
        } else if (ID.equals(eventField)) {
            sEventValue = value.getId();
        } else if (CTIME.equals(eventField)) {
            lEventValue = value.getCtime();
        } else if (EVENT_TEXT.equals(eventField)) {
            sEventValue = value.getEventText();
        } else if (eventField.startsWith(CONTEXT)) {
            // We get the key from context.<key> string
            String key = eventField.substring(8);
            sEventValue = value.getContext().get(key);
        } else if (eventField.startsWith(TAGS)) {
            // We get the key from tags.<key> string
            String key = eventField.substring(5);
            sEventValue = value.getTags().get(key);
        }
        if (sEventValue == null && lEventValue == null) {
            return false;
        }
        if (constant == null) {
            return false;
        }
        int constantLength = constant.length();
        if (constant.charAt(0) == '\'' && constant.charAt(constantLength - 1) == '\'') {
            sConstantValue = constant.substring(1, constantLength - 1);
        } else if (constant.charAt(0) == '\'' && constant.charAt(constantLength - 1) != '\'') {
            return false;
        } else if (constant.charAt(0) != '\'' && constant.charAt(constantLength - 1) == '\'') {
            return false;
        } else  {
            dConstantValue = Double.valueOf(constant);
        }

        if (EQ.equals(operator)) {
            if (sEventValue != null && sConstantValue != null) {
                return sEventValue.equals(sConstantValue);
            }
            if (lEventValue != null && dConstantValue != null) {
                return lEventValue.longValue() == dConstantValue.doubleValue();
            }
            return false;
        } else if (NON_EQ.equals(operator)) {
            if (sEventValue != null && sConstantValue != null) {
                return !sEventValue.equals(sConstantValue);
            }
            if (lEventValue != null && dConstantValue != null) {
                return lEventValue.longValue() != dConstantValue.doubleValue();
            }
            return false;
        } else if (STARTS.equals(operator)) {
            if (sEventValue != null && sConstantValue != null) {
                return sEventValue.startsWith(sConstantValue);
            }
            return false;
        } else if (ENDS.equals(operator)) {
            if (sEventValue != null && sConstantValue != null) {
                return sEventValue.endsWith(sConstantValue);
            }
            return false;
        } else if (CONTAINS.equals(operator)) {
            if (sEventValue != null && sConstantValue != null) {
                return sEventValue.contains(sConstantValue);
            }
            return false;
        } else if (MATCHES.equals(operator)) {
            if (sEventValue != null && sConstantValue != null) {
                return sEventValue.matches(sConstantValue);
            }
            return false;
        } else if (GT.equals(operator)) {
            Double dEventValue = lEventValue != null ? lEventValue.doubleValue() : null;
            dEventValue = sEventValue != null ? Double.valueOf(sEventValue) : dEventValue;
            if (dEventValue != null && dConstantValue != null) {
                return dEventValue > dConstantValue;
            }
            return false;
        } else if (GTE.equals(operator)) {
            Double dEventValue = lEventValue != null ? lEventValue.doubleValue() : null;
            dEventValue = sEventValue != null ? Double.valueOf(sEventValue) : dEventValue;
            if (dEventValue != null && dConstantValue != null) {
                return dEventValue >= dConstantValue;
            }
            return false;
        } else if (LT.equals(operator)) {
            Double dEventValue = lEventValue != null ? lEventValue.doubleValue() : null;
            dEventValue = sEventValue != null ? Double.valueOf(sEventValue) : dEventValue;
            if (dEventValue != null && dConstantValue != null) {
                return dEventValue < dConstantValue;
            }
            return false;
        } else if (LTE.equals(operator)) {
            Double dEventValue = lEventValue != null ? lEventValue.doubleValue() : null;
            dEventValue = sEventValue != null ? Double.valueOf(sEventValue) : dEventValue;
            if (dEventValue != null && dConstantValue != null) {
                return dEventValue <= dConstantValue;
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EventCondition that = (EventCondition) o;

        return !(expression != null ? !expression.equals(that.expression) : that.expression != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (expression != null ? expression.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EventCondition{" +
                "expression='" + expression + '\'' +
                '}';
    }
}
