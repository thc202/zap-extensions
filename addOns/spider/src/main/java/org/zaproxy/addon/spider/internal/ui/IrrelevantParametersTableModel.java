/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2022 The ZAP Development Team
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
 */
package org.zaproxy.addon.spider.internal.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.parosproxy.paros.Constant;
import org.zaproxy.addon.spider.internal.IrrelevantParameter;
import org.zaproxy.zap.view.AbstractMultipleOptionsTableModel;

@SuppressWarnings("serial")
class IrrelevantParametersTableModel
        extends AbstractMultipleOptionsTableModel<IrrelevantParameter> {

    private static final long serialVersionUID = -5411351965957264957L;

    private static final String[] COLUMN_NAMES = {
        Constant.messages.getString("spider.options.irrelevantparameter.table.header.enabled"),
        Constant.messages.getString("spider.options.irrelevantparameter.table.header.regex"),
        Constant.messages.getString("spider.options.irrelevantparameter.table.header.name")
    };

    private static final int COLUMN_COUNT = COLUMN_NAMES.length;

    private List<IrrelevantParameter> irrelevantParameters;

    public IrrelevantParametersTableModel() {
        irrelevantParameters = new ArrayList<>(5);
    }

    @Override
    public String getColumnName(int col) {
        return COLUMN_NAMES[col];
    }

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public int getRowCount() {
        return irrelevantParameters.size();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return getElement(rowIndex).isEnabled();
            case 1:
                return getElement(rowIndex).isRegex();
            case 2:
                return getElement(rowIndex).getName();
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0 && aValue instanceof Boolean) {
            irrelevantParameters.get(rowIndex).setEnabled((Boolean) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        if (c == 0 || c == 1) {
            return Boolean.class;
        }
        return String.class;
    }

    public void setElements(List<IrrelevantParameter> irrelevantParameters) {
        this.irrelevantParameters =
                irrelevantParameters.stream()
                        .map(IrrelevantParameter::new)
                        .collect(Collectors.toCollection(ArrayList::new));

        fireTableDataChanged();
    }

    @Override
    public List<IrrelevantParameter> getElements() {
        return irrelevantParameters;
    }
}
