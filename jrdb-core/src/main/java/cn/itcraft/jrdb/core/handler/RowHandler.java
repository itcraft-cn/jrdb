package cn.itcraft.jrdb.core.handler;

public interface RowHandler {

    void onNextRow(byte[] rawRow, RowMeta meta);
}