package com.dbmmap.core.handler;

public interface RowHandler {

    void onNextRow(byte[] rawRow, RowMeta meta);
}