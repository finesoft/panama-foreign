/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */



package jdk.tools.jaotc;

import jdk.tools.jaotc.binformat.BinaryContainer;
import jdk.tools.jaotc.binformat.Relocation;
import jdk.tools.jaotc.binformat.Relocation.RelocType;
import jdk.tools.jaotc.binformat.Symbol;

import jdk.vm.ci.code.site.Mark;

final class MarkProcessor {

    private final BinaryContainer binaryContainer;

    MarkProcessor(DataBuilder dataBuilder) {
        binaryContainer = dataBuilder.getBinaryContainer();
    }

    /**
     * Parse a {@link Mark} generated by the compiler and create all needed binary section
     * constructs.
     *
     * @param methodInfo compiled method info
     * @param mark mark being processed
     */
    @SuppressWarnings("fallthrough")
    void process(CompiledMethodInfo methodInfo, Mark mark) {
        MarkId markId = MarkId.getEnum((int) mark.id);
        switch (markId) {
            case EXCEPTION_HANDLER_ENTRY:
            case DEOPT_HANDLER_ENTRY:
                break;
            case POLL_FAR:
            case POLL_RETURN_FAR:
                if (binaryContainer.getThreadLocalHandshakes()) {
                    // skip relocation
                    break;
                }
                // fallthrough
            case CARD_TABLE_ADDRESS:
            case HEAP_TOP_ADDRESS:
            case HEAP_END_ADDRESS:
            case NARROW_KLASS_BASE_ADDRESS:
            case NARROW_OOP_BASE_ADDRESS:
            case CRC_TABLE_ADDRESS:
            case LOG_OF_HEAP_REGION_GRAIN_BYTES:
            case INLINE_CONTIGUOUS_ALLOCATION_SUPPORTED:
                String vmSymbolName;
                switch (markId) {
                    case POLL_FAR:
                    case POLL_RETURN_FAR:
                        vmSymbolName = BinaryContainer.getPollingPageSymbolName();
                        break;
                    case CARD_TABLE_ADDRESS:
                        vmSymbolName = BinaryContainer.getCardTableAddressSymbolName();
                        break;
                    case HEAP_TOP_ADDRESS:
                        vmSymbolName = BinaryContainer.getHeapTopAddressSymbolName();
                        break;
                    case HEAP_END_ADDRESS:
                        vmSymbolName = BinaryContainer.getHeapEndAddressSymbolName();
                        break;
                    case NARROW_KLASS_BASE_ADDRESS:
                        vmSymbolName = BinaryContainer.getNarrowKlassBaseAddressSymbolName();
                        break;
                    case NARROW_OOP_BASE_ADDRESS:
                        vmSymbolName = BinaryContainer.getNarrowOopBaseAddressSymbolName();
                        break;
                    case CRC_TABLE_ADDRESS:
                        vmSymbolName = BinaryContainer.getCrcTableAddressSymbolName();
                        break;
                    case LOG_OF_HEAP_REGION_GRAIN_BYTES:
                        vmSymbolName = BinaryContainer.getLogOfHeapRegionGrainBytesSymbolName();
                        break;
                    case INLINE_CONTIGUOUS_ALLOCATION_SUPPORTED:
                        vmSymbolName = BinaryContainer.getInlineContiguousAllocationSupportedSymbolName();
                        break;
                    default:
                        throw new InternalError("Unhandled mark: " + mark);
                }
                String s = "got." + vmSymbolName;
                Symbol gotSymbol = binaryContainer.getGotSymbol(s);
                assert gotSymbol != null : " Processing Mark: Encountered undefined got symbol for  " + mark;
                final int textBaseOffset = methodInfo.getTextSectionOffset();
                final int textOffset = textBaseOffset + mark.pcOffset;
                Relocation reloc = new Relocation(textOffset, RelocType.EXTERNAL_PLT_TO_GOT, 8, binaryContainer.getCodeContainer(), gotSymbol);
                binaryContainer.addRelocation(reloc);
                break;
            case VERIFIED_ENTRY:
            case UNVERIFIED_ENTRY:
            case OSR_ENTRY:
            case INVOKEINTERFACE:
            case INVOKEVIRTUAL:
            case INVOKESTATIC:
            case INVOKESPECIAL:
            case INLINE_INVOKE:
            case POLL_NEAR:
            case POLL_RETURN_NEAR:
                // Nothing to do.
                break;
            default:
                throw new InternalError("Unexpected mark found: " + mark);
        }
    }
}
