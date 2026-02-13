package com.mrpowergamerbr.butterscotch.vm

object Opcodes {
    const val CONV = 0x07
    const val MUL = 0x08
    const val DIV = 0x09
    const val REM = 0x0A
    const val MOD = 0x0B
    const val ADD = 0x0C
    const val SUB = 0x0D
    const val AND = 0x0E
    const val OR = 0x0F
    const val XOR = 0x10
    const val NEG = 0x11
    const val NOT = 0x12
    const val SHL = 0x13
    const val SHR = 0x14
    const val CMP = 0x15
    const val POP = 0x45
    const val PUSHI = 0x84
    const val DUP = 0x86
    const val RET = 0x9C
    const val EXIT = 0x9D
    const val POPZ = 0x9E
    const val B = 0xB6
    const val BT = 0xB7
    const val BF = 0xB8
    const val PUSHENV = 0xBA
    const val POPENV = 0xBB
    const val PUSH = 0xC0
    const val PUSHLOC = 0xC1
    const val PUSHGLB = 0xC2
    const val PUSHBLTN = 0xC3
    const val CALL = 0xD9
    const val BREAK = 0xFF
}

object DataTypes {
    const val DOUBLE = 0
    const val FLOAT = 1
    const val INT32 = 2
    const val INT64 = 3
    const val BOOLEAN = 4
    const val VARIABLE = 5
    const val STRING = 6
    const val INT16 = 0x0F
}

object ComparisonTypes {
    const val LT = 1
    const val LTE = 2
    const val EQ = 3
    const val NEQ = 4
    const val GTE = 5
    const val GT = 6
}

object InstanceTypes {
    const val SELF = -1
    const val OTHER = -2
    const val ALL = -3
    const val NOONE = -4
    const val GLOBAL = -5
    const val BUILTIN = -6
    const val LOCAL = -7
    const val STACKTOP = -9
}

class Instruction(
    val opcode: Int,
    val type1: Int,
    val type2: Int,
    val extra: Int,         // bytes 0-1 as i16 (instance type for var, argc for call, comparison for cmp)
    val rawOperand: Int,    // full 24-bit operand

    // Resolved references (set during reference chain resolution)
    var variableIndex: Int = -1,
    var variableType: Int = 0,   // 0x00=Array, 0xA0=Normal, etc.
    var functionIndex: Int = -1,

    // Inline values for Push
    var intValue: Int = 0,
    var longValue: Long = 0,
    var doubleValue: Double = 0.0,
    var floatValue: Float = 0f,
    var stringIndex: Int = -1,
) {
    val instanceType: Int get() = extra   // for variable access (signed i16)
    val argCount: Int get() = extra and 0xFFFF  // for Call (unsigned)
    val comparisonType: Int get() = (rawOperand shr 8) and 0xFF  // byte 1 for Cmp
    val branchOffset: Int get() {
        // 23-bit signed offset in bits 0-22
        val raw = rawOperand and 0x7FFFFF
        return if (raw and 0x400000 != 0) raw or (0xFF800000.toInt()) else raw
    }
}

/**
 * Decodes raw bytecode into a list of Instructions.
 * Returns the list and a map of (bytecodeLocalOffset -> instructionIndex).
 */
fun decodeBytecode(bytecode: ByteArray): Pair<List<Instruction>, Map<Int, Int>> {
    val instructions = mutableListOf<Instruction>()
    val offsetMap = mutableMapOf<Int, Int>()
    var pos = 0

    fun readU8(offset: Int): Int = bytecode[offset].toInt() and 0xFF
    fun readI16(offset: Int): Int {
        val lo = bytecode[offset].toInt() and 0xFF
        val hi = bytecode[offset + 1].toInt()
        return (hi shl 8) or lo
    }
    fun readU16(offset: Int): Int = readI16(offset) and 0xFFFF
    fun readI32(offset: Int): Int {
        val b0 = bytecode[offset].toInt() and 0xFF
        val b1 = bytecode[offset + 1].toInt() and 0xFF
        val b2 = bytecode[offset + 2].toInt() and 0xFF
        val b3 = bytecode[offset + 3].toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }
    fun readI64(offset: Int): Long {
        val lo = readI32(offset).toLong() and 0xFFFFFFFFL
        val hi = readI32(offset + 4).toLong()
        return lo or (hi shl 32)
    }
    fun readF32(offset: Int): Float = Float.fromBits(readI32(offset))
    fun readF64(offset: Int): Double = Double.fromBits(readI64(offset))

    while (pos < bytecode.size) {
        val instrOffset = pos
        offsetMap[instrOffset] = instructions.size

        val word = readI32(pos)
        val opcode = (word shr 24) and 0xFF
        val type1 = (word shr 16) and 0x0F
        val type2 = (word shr 20) and 0x0F
        val extra = readI16(pos)   // bytes 0-1 as signed i16
        val operand24 = word and 0x00FFFFFF
        pos += 4

        val instr = Instruction(
            opcode = opcode,
            type1 = type1,
            type2 = type2,
            extra = extra,
            rawOperand = operand24,
        )

        when (opcode) {
            Opcodes.PUSH -> {
                // Push constant. type1 = data type of the constant.
                when (type1) {
                    DataTypes.DOUBLE -> {
                        instr.doubleValue = readF64(pos)
                        pos += 8
                    }
                    DataTypes.FLOAT -> {
                        instr.floatValue = readF32(pos)
                        pos += 4
                    }
                    DataTypes.INT32 -> {
                        instr.intValue = readI32(pos)
                        pos += 4
                    }
                    DataTypes.INT64 -> {
                        instr.longValue = readI64(pos)
                        pos += 8
                    }
                    DataTypes.BOOLEAN -> {
                        instr.intValue = readI32(pos)
                        pos += 4
                    }
                    DataTypes.STRING -> {
                        instr.stringIndex = readI32(pos)
                        pos += 4
                    }
                    DataTypes.INT16 -> {
                        // Value already in extra (bytes 0-1)
                        instr.intValue = extra
                    }
                    DataTypes.VARIABLE -> {
                        // Variable reference (same format as PUSHLOC/PUSHGLB/PUSHBLTN)
                        val refValue = readI32(pos)
                        instr.variableType = ((refValue shr 24) and 0xF8)
                        pos += 4
                    }
                    else -> {
                        // Unknown type, read 4 bytes
                        instr.intValue = readI32(pos)
                        pos += 4
                    }
                }
            }

            Opcodes.PUSHLOC, Opcodes.PUSHGLB, Opcodes.PUSHBLTN -> {
                // Push variable: 8 bytes total. Next 4 bytes = variable reference (chain link or resolved)
                val refValue = readI32(pos)
                instr.variableType = ((refValue shr 24) and 0xF8)
                // The variable index will be set during chain resolution
                pos += 4
            }

            Opcodes.POP -> {
                // Pop (store to variable): 8 bytes total
                val refValue = readI32(pos)
                instr.variableType = ((refValue shr 24) and 0xF8)
                pos += 4
            }

            Opcodes.CALL -> {
                // Call: 8 bytes total. Next 4 bytes = function reference
                // argc in extra (bytes 0-1), return type in type1 (byte 2)
                pos += 4 // skip function reference (resolved during chain walk)
            }

            Opcodes.PUSHI -> {
                // PushI: 4 bytes total. Value in bytes 0-1 (extra), already captured.
                instr.intValue = extra
            }

            // All other instructions are 4 bytes (already consumed)
        }

        instructions.add(instr)
    }

    return Pair(instructions, offsetMap)
}
