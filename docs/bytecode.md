# GameMaker Bytecode 16 Instruction Set

Reference: [UndertaleModTool/Models/UndertaleCode.cs](https://github.com/UnderminersTeam/UndertaleModTool/blob/master/UndertaleModLib/Models/UndertaleCode.cs)

## VM Architecture

- **Stack-based** virtual machine
- All values on the stack are typed (Double, Float, Int32, Int64, Boolean, Variable, String, Int16)
- Real numbers are 64-bit doubles
- Strings are reference-counted
- Arrays use 2D indexing (`array[x, y]`) with copy-on-write semantics in GM:S 1.4

## Instruction Encoding

Each instruction is **4 bytes** minimum:

```
Byte 0-2: Operand data (24 bits, little-endian) - meaning depends on instruction type
Byte 3:   Opcode
```

Some instructions are **8 bytes** (double-word) with an additional 4-byte operand.

## Data Types

| Value | Name     | Size    |
|-------|----------|---------|
| 0x0   | Double   | 8 bytes |
| 0x1   | Float    | 4 bytes |
| 0x2   | Int32    | 4 bytes |
| 0x3   | Int64    | 8 bytes |
| 0x4   | Boolean  | 4 bytes |
| 0x5   | Variable | 4 bytes |
| 0x6   | String   | 4 bytes (index) |
| 0xF   | Int16    | 2 bytes (in 24-bit field) |

## Instance Types (for variable access)

| Value | Name        |
|-------|-------------|
| -1    | Self        |
| -2    | Other       |
| -3    | All         |
| -4    | Noone       |
| -5    | Global      |
| -6    | Builtin     |
| -7    | Local       |
| -9    | Stacktop    |
| >= 0  | Object ID   |

## Instruction Types

### Single-byte operand instructions (4 bytes total)

#### Comparison/Branch (type encoded in bytes 0-1)
| Opcode | Name | Description |
|--------|------|-------------|
| 0x07   | Conv | Convert TOS from Type1 to Type2 |
| 0x08   | Mul  | Multiply: push(pop() * pop()) |
| 0x09   | Div  | Divide |
| 0x0A   | Rem  | Integer remainder |
| 0x0B   | Mod  | Modulo |
| 0x0C   | Add  | Add |
| 0x0D   | Sub  | Subtract |
| 0x0E   | And  | Bitwise AND |
| 0x0F   | Or   | Bitwise OR |
| 0x10   | Xor  | Bitwise XOR |
| 0x11   | Neg  | Negate TOS |
| 0x12   | Not  | Logical NOT |
| 0x13   | Shl  | Shift left |
| 0x14   | Shr  | Shift right |

#### Comparison (produces boolean)
| Opcode | Name | Description |
|--------|------|-------------|
| 0x15   | Cmp  | Compare: `pop() <op> pop()`. Comparison type in bits 8-15 |

Comparison sub-types (encoded in byte 1):
| Value | Comparison |
|-------|------------|
| 1     | < (LT)     |
| 2     | <= (LTE)   |
| 3     | == (EQ)    |
| 4     | != (NEQ)   |
| 5     | >= (GTE)   |
| 6     | > (GT)     |

### Stack operations
| Opcode | Name | Description |
|--------|------|-------------|
| 0x41   | Dup  | Duplicate top of stack |
| 0x45   | Ret  | Return from function (pop return value) |
| 0x46   | Exit | Exit current script/event |
| 0x9C   | Pop  | Pop value and store to variable |
| 0x9D   | Popz | Pop and discard |

### Push instructions (8 bytes for non-Int16)
| Opcode | Name     | Description |
|--------|----------|-------------|
| 0x84   | PushI    | Push Int16 (value in 24-bit field) |
| 0xC0   | Push     | Push constant (type in byte 0, value in next 4/8 bytes) |
| 0xC1   | PushLoc  | Push local variable |
| 0xC2   | PushGlb  | Push global variable |
| 0xC3   | PushBltn | Push builtin variable |

### Branch instructions
| Opcode | Name    | Description |
|--------|---------|-------------|
| 0xB6   | B       | Unconditional branch (24-bit signed offset) |
| 0xB7   | Bt      | Branch if true |
| 0xB8   | Bf      | Branch if false |
| 0xBA   | PushEnv | Push environment (enter `with` block) |
| 0xBB   | PopEnv  | Pop environment (exit `with` block) |

### Call instruction (8 bytes)
| Opcode | Name | Description |
|--------|------|-------------|
| 0xD9   | Call | Call function. Args count in bytes 0-1, function ref in next 4 bytes |

### Break/Extended (GM:S 2.3+, not needed for Undertale)
| Opcode | Name  | Description |
|--------|-------|-------------|
| 0xFF   | Break | Extended operations (not used in BC16) |

## Push Instruction Details

### PushI (0x84) - Push Int16
- The 24-bit field (bytes 0-2) contains the signed 16-bit value
- 4 bytes total

### Push (0xC0) - Push Constant
- Byte 0: Data type
- Bytes 1-2: unused/zero
- Byte 3: 0xC0
- Next 4 or 8 bytes: the constant value
  - Double: 8 bytes
  - Float: 4 bytes
  - Int32: 4 bytes
  - Int64: 8 bytes
  - String: 4 bytes (string index)
  - Boolean: 4 bytes

### PushLoc/PushGlb/PushBltn (0xC1/0xC2/0xC3) - Push Variable
- Type1 = Variable (0x5)
- 24-bit field: instance type (as i16) in lower 16 bits
- Next 4 bytes: variable reference (index, patched by VARI chunk)

## Pop Instruction (0x9C)

Stores TOS to a variable:
```
Byte 0: Type1 (destination type - usually Variable 0x5)
Byte 1: Type2 (source type - type of value being stored)
Byte 2: (part of instance type)
Byte 3: 0x9C
Next 4 bytes: variable reference
```

## Call Instruction (0xD9)

```
Bytes 0-1: argument count (u16)
Byte 2: Type1 (return type)
Byte 3: 0xD9
Next 4 bytes: function reference (patched by FUNC chunk)
```

## Branch Instructions (0xB6, 0xB7, 0xB8)

The 24-bit operand is a **signed offset** in 4-byte units from the current instruction's address.

## Variable/Function Reference Chains

In the raw bytecode, variable and function references form linked lists:
- The VARI chunk has `firstOccurrenceOffset` pointing to the first instruction using that variable
- Each instruction's reference field points to the next occurrence
- The last occurrence stores the variable/function name string index

During loading, these chains must be resolved to map each reference to its variable/function definition.

## Execution Model

1. Each code entry starts with `localsCount` local variable slots
2. Arguments are passed on the stack (popped by callee)
3. Return value is pushed onto the stack
4. Branch offsets are relative to the current instruction position
5. `PushEnv`/`PopEnv` implement `with(obj) { ... }` by changing the current `self` context
