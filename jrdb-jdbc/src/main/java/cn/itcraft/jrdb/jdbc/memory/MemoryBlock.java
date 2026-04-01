package cn.itcraft.jrdb.jdbc.memory;

import sun.misc.Unsafe;

/**
 * 内存块对象，表示从内存池分配的一块连续堆外内存。
 * 
 * <p>本类提供线程安全的基本类型和字节数组的读写操作，是 jrdb 框架堆外内存管理的核心组件。
 * 每个内存块由内存池统一管理，通过槽位标识（slotId）进行追踪和回收。</p>
 * 
 * <h3>核心特性：</h3>
 * <ul>
 *   <li>堆外内存存储，零 GC 压力</li>
 *   <li>提供安全访问方法（带边界检查）和快速访问方法（无边界检查）</li>
 *   <li>支持 byte、int、long 基本类型及字节数组的读写</li>
 *   <li>线程安全，可在多线程环境下使用</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * MemoryBlock block = memoryPool.allocate(1024);
 * block.writeInt(0, 100);
 * block.writeLong(4, 123456789L);
 * int value = block.readInt(0);
 * }</pre>
 * 
 * @author jrdb
 * @see MemoryPool
 * @since 1.0
 */
public class MemoryBlock {
    
    /**
     * 内存块的基地址。
     * 
     * <p>这是堆外内存的起始地址，通过 {@link Unsafe} 进行直接内存访问。
     * 地址值在内存块生命周期内保持不变。</p>
     */
    private final long address;
    
    /**
     * 内存块的大小，单位为字节。
     * 
     * <p>定义了本内存块可访问的内存范围，所有读写操作必须在此范围内进行。
     * 安全访问方法会检查偏移量是否超出此大小限制。</p>
     */
    private final int size;
    
    /**
     * 内存块在内存池中的槽位标识。
     * 
     * <p>槽位标识由 {@link MemoryPool} 分配，用于内存池内部的索引管理和内存块的回收追踪。
     * 同一内存池内每个内存块的槽位标识唯一。</p>
     */
    private final int slotId;
    
    /**
     * Unsafe 实例，用于执行底层内存操作。
     * 
     * <p>通过 Unsafe 提供的本地方法直接访问堆外内存，绕过 JVM 堆内存管理，
     * 实现高性能的内存读写操作。</p>
     */
    private final Unsafe unsafe;
    
    /**
     * 创建一个新的内存块实例。
     * 
     * <p>此构造函数为包级私有，仅由 {@link MemoryPool} 在分配内存时调用。
     * 调用者需确保传入的地址指向有效的堆外内存区域。</p>
     *
     * @param address 内存块的基地址，必须为有效的堆外内存地址
     * @param size 内存块的大小，单位为字节，必须大于 0
     * @param slotId 内存池分配的槽位标识，用于内存池管理
     * @param unsafe Unsafe 实例，用于底层内存操作，不能为 null
     * @throws IllegalArgumentException 如果 unsafe 参数为 null
     */
    MemoryBlock(long address, int size, int slotId, Unsafe unsafe) {
        if (unsafe == null) {
            throw new IllegalArgumentException("Unsafe instance cannot be null");
        }
        this.address = address;
        this.size = size;
        this.slotId = slotId;
        this.unsafe = unsafe;
    }
    
    /**
     * 获取内存块的基地址。
     *
     * @return 内存块的基地址值
     */
    public long getAddress() { 
        return address; 
    }
    
    /**
     * 获取内存块的大小。
     *
     * @return 内存块的大小，单位为字节
     */
    public int getSize() { 
        return size; 
    }
    
    /**
     * 获取内存块在内存池中的槽位标识。
     *
     * @return 槽位标识
     */
    public int getSlotId() { 
        return slotId; 
    }
    
    /**
     * 从指定偏移量读取一个字节值。
     * 
     * <p>此方法为安全访问方法，会检查偏移量是否在有效范围内。
     * 如需更高性能且能确保偏移量合法的场景，可使用 {@link #readByte(int)} 的快速版本。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @return 指定偏移量位置的字节值
     * @throws IndexOutOfBoundsException 如果偏移量超出内存块边界
     */
    public byte readByte(int offset) {
        checkOffset(offset, 1);
        return unsafe.getByte(address + offset);
    }
    
    /**
     * 在指定偏移量写入一个字节值。
     * 
     * <p>此方法为安全访问方法，会检查偏移量是否在有效范围内。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @param value 要写入的字节值
     * @throws IndexOutOfBoundsException 如果偏移量超出内存块边界
     */
    public void writeByte(int offset, byte value) {
        checkOffset(offset, 1);
        unsafe.putByte(address + offset, value);
    }
    
    /**
     * 从指定偏移量读取一个 int 值。
     * 
     * <p>此方法为安全访问方法，会检查偏移量及后续 4 字节是否在有效范围内。
     * int 值占用 4 字节内存空间。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @return 指定偏移量位置的 int 值
     * @throws IndexOutOfBoundsException 如果偏移量或偏移量+4超出内存块边界
     */
    public int readInt(int offset) {
        checkOffset(offset, 4);
        return unsafe.getInt(address + offset);
    }
    
    /**
     * 在指定偏移量写入一个 int 值。
     * 
     * <p>此方法为安全访问方法，会检查偏移量及后续 4 字节是否在有效范围内。
     * int 值占用 4 字节内存空间。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @param value 要写入的 int 值
     * @throws IndexOutOfBoundsException 如果偏移量或偏移量+4超出内存块边界
     */
    public void writeInt(int offset, int value) {
        checkOffset(offset, 4);
        unsafe.putInt(address + offset, value);
    }
    
    /**
     * 从指定偏移量读取一个 long 值。
     * 
     * <p>此方法为安全访问方法，会检查偏移量及后续 8 字节是否在有效范围内。
     * long 值占用 8 字节内存空间。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @return 指定偏移量位置的 long 值
     * @throws IndexOutOfBoundsException 如果偏移量或偏移量+8超出内存块边界
     */
    public long readLong(int offset) {
        checkOffset(offset, 8);
        return unsafe.getLong(address + offset);
    }
    
    /**
     * 在指定偏移量写入一个 long 值。
     * 
     * <p>此方法为安全访问方法，会检查偏移量及后续 8 字节是否在有效范围内。
     * long 值占用 8 字节内存空间。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @param value 要写入的 long 值
     * @throws IndexOutOfBoundsException 如果偏移量或偏移量+8超出内存块边界
     */
    public void writeLong(int offset, long value) {
        checkOffset(offset, 8);
        unsafe.putLong(address + offset, value);
    }
    
    /**
     * 从指定偏移量读取字节数组到目标数组。
     * 
     * <p>此方法为安全访问方法，会检查偏移量及读取长度是否在有效范围内。
     * 读取的字节数由目标数组的长度决定。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @param dest 目标字节数组，用于存储读取的数据
     * @throws IndexOutOfBoundsException 如果偏移量+dest.length超出内存块边界
     */
    public void readBytes(int offset, byte[] dest) {
        checkOffset(offset, dest.length);
        unsafe.copyMemory(null, address + offset, dest, Unsafe.ARRAY_BYTE_BASE_OFFSET, dest.length);
    }
    
    /**
     * 将源字节数组写入指定偏移量位置。
     * 
     * <p>此方法为安全访问方法，会检查偏移量及写入长度是否在有效范围内。
     * 写入的字节数由源数组的长度决定。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @param src 源字节数组，包含要写入的数据
     * @throws IndexOutOfBoundsException 如果偏移量+src.length超出内存块边界
     */
    public void writeBytes(int offset, byte[] src) {
        checkOffset(offset, src.length);
        unsafe.copyMemory(src, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, address + offset, src.length);
    }
    
    /**
     * 快速写入 int 值，不执行边界检查。
     * 
     * <p>此方法跳过边界检查以获得更高的性能。调用者必须确保偏移量合法
     * （offset >= 0 且 offset + 4 <= size），否则可能导致 JVM 崩溃或内存损坏。</p>
     * 
     * <p><b>警告：</b>仅在对性能有极高要求且能保证偏移量合法性的场景下使用，
     * 通常在对同一内存块进行连续操作时使用。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @param value 要写入的 int 值
     */
    public void writeIntFast(int offset, int value) {
        unsafe.putInt(address + offset, value);
    }
    
    /**
     * 快速写入 long 值，不执行边界检查。
     * 
     * <p>此方法跳过边界检查以获得更高的性能。调用者必须确保偏移量合法
     * （offset >= 0 且 offset + 8 <= size），否则可能导致 JVM 崩溃或内存损坏。</p>
     * 
     * <p><b>警告：</b>仅在对性能有极高要求且能保证偏移量合法性的场景下使用，
     * 通常在对同一内存块进行连续操作时使用。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @param value 要写入的 long 值
     */
    public void writeLongFast(int offset, long value) {
        unsafe.putLong(address + offset, value);
    }
    
    /**
     * 快速读取 int 值，不执行边界检查。
     * 
     * <p>此方法跳过边界检查以获得更高的性能。调用者必须确保偏移量合法
     * （offset >= 0 且 offset + 4 <= size），否则可能导致 JVM 崩溃或读取到非法数据。</p>
     * 
     * <p><b>警告：</b>仅在对性能有极高要求且能保证偏移量合法性的场景下使用，
     * 通常在对同一内存块进行连续操作时使用。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @return 指定偏移量位置的 int 值
     */
    public int readIntFast(int offset) {
        return unsafe.getInt(address + offset);
    }
    
    /**
     * 快速读取 long 值，不执行边界检查。
     * 
     * <p>此方法跳过边界检查以获得更高的性能。调用者必须确保偏移量合法
     * （offset >= 0 且 offset + 8 <= size），否则可能导致 JVM 崩溃或读取到非法数据。</p>
     * 
     * <p><b>警告：</b>仅在对性能有极高要求且能保证偏移量合法性的场景下使用，
     * 通常在对同一内存块进行连续操作时使用。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @return 指定偏移量位置的 long 值
     */
    public long readLongFast(int offset) {
        return unsafe.getLong(address + offset);
    }
    
    /**
     * 快速写入字节数组，不执行边界检查。
     * 
     * <p>此方法跳过边界检查以获得更高的性能。调用者必须确保偏移量合法
     * （offset >= 0 且 offset + src.length <= size），否则可能导致 JVM 崩溃或内存损坏。</p>
     * 
     * <p><b>警告：</b>仅在对性能有极高要求且能保证偏移量合法性的场景下使用，
     * 通常在对同一内存块进行连续操作时使用。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @param src 源字节数组，包含要写入的数据
     */
    public void writeBytesFast(int offset, byte[] src) {
        unsafe.copyMemory(src, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, address + offset, src.length);
    }
    
    /**
     * 快速读取字节数组，不执行边界检查。
     * 
     * <p>此方法跳过边界检查以获得更高的性能。调用者必须确保偏移量合法
     * （offset >= 0 且 offset + dest.length <= size），否则可能导致 JVM 崩溃或读取到非法数据。</p>
     * 
     * <p><b>警告：</b>仅在对性能有极高要求且能保证偏移量合法性的场景下使用，
     * 通常在对同一内存块进行连续操作时使用。</p>
     *
     * @param offset 字节偏移量，相对于内存块基地址
     * @param dest 目标字节数组，用于存储读取的数据
     */
    public void readBytesFast(int offset, byte[] dest) {
        unsafe.copyMemory(null, address + offset, dest, Unsafe.ARRAY_BYTE_BASE_OFFSET, dest.length);
    }
    
    /**
     * 检查偏移量和访问长度是否在内存块边界内。
     * 
     * <p>此私有方法用于所有安全访问方法的边界验证，确保不会发生越界访问。
     * 检查条件：offset >= 0 且 offset + length <= size。</p>
     *
     * @param offset 字节偏移量
     * @param length 访问长度，单位为字节
     * @throws IndexOutOfBoundsException 如果偏移量或访问范围超出内存块边界
     */
    private void checkOffset(int offset, int length) {
        if (offset < 0 || offset + length > size) {
            throw new IndexOutOfBoundsException(
                "offset=" + offset + ", length=" + length + ", size=" + size);
        }
    }
}