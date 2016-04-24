package edu.psu.ist.plato.kaiming.elf

import scodec.bits._
import scodec.codecs._
import scodec.{bits => _, _}

import java.nio.ByteBuffer

final class Elf(input: ByteVector) {
  
  val headerSize = 52
  val identifierSize = 16
  val progHeaderSize = 32
  val secHeaderSize = 40 
  
  /**
   * #define EI_NIDENT 16
     // N = 32, 64
     typedef struct {
         unsigned char e_ident[EI_NIDENT];
         uint16_t      e_type;
         uint16_t      e_machine;
         uint32_t      e_version;
         ElfN_Addr     e_entry;
         ElfN_Off      e_phoff;
         ElfN_Off      e_shoff;
         uint32_t      e_flags;
         uint16_t      e_ehsize;
         uint16_t      e_phentsize;
         uint16_t      e_phnum;
         uint16_t      e_shentsize;
         uint16_t      e_shnum;
         uint16_t      e_shstrndx;
     } ElfN_Ehdr;
   * 
   */
  class Header(identifier : ByteVector, bytes: ByteVector) {
    
    val size = headerSize
    
    private val uint16_t = if (identifier.get(5) == 1) uint16L else uint16
    private val uint32_t = if (identifier.get(5) == 1) uint32L else uint32

    val codec = 
      uint16_t ~ // e_type
      uint16_t ~ // e_machine
      uint32_t ~ // e_version
      uint32_t ~ // e_entry
      uint32_t ~ // e_phoff
      uint32_t ~ // e_shoff
      uint32_t ~ // e_flags
      uint16_t ~ // e_ehsize
      uint16_t ~ // e_phentsize
      uint16_t ~ // e_phnum
      uint16_t ~ // e_shentsize
      uint16_t ~ // e_shnum
      uint16_t   // e_shstrndx
    
    val packedFields = codec.decode(bytes.bits) match {
        case Attempt.Successful(DecodeResult(list, _)) => list
        case Attempt.Failure(cause) => throw new IllegalArgumentException(cause.messageWithContext)
      }
    
    val e_indent = identifier
    
    val ((((((((((((
        e_type,
        e_machine),
        e_version),
        e_entry),
        e_phoff),
        e_shoff),
        e_flags),
        e_ehsize),
        e_phentsize),
        e_phnum),
        e_shentsize),
        e_shnum),
        e_shstrndx) = packedFields
    
    def toHex(l: Long) = java.lang.Long.toHexString(l)
    
    private def classStrings: Int => String = {
      case 0 => "Invalid"
      case 1 => "ELF32"
      case 2 => "ELF64"
      case _ => "Elf.scala: unknown"
    }
    
    private val dataStrings: Int => String =  {
      case 0 => "Unknown data format"
      case 1 => "Two's complement, little-endian"
      case 2 => "Two's complement, big-endian"
      case _ => "Elf.scala: unknown"
    }
        
    private val elfVersionStrings: Int => String = {
      case 0 => "Invlaid"
      case 1 => "Current"
      case _ => "Elf.scala: unknown"
    }
    
    private val osAbiStrings: Int => String = {
      case 0 => "UNIX System V ABI"
      case 1 => "HP-UX"
      case 2 => "NetBSD"
      case 3 => "Object uses GNU ELF extensions"
      case 6 => "Sun Solaris"
      case 7 => "IBM AIX"
      case 8 => "SGI Irix"
      case 9 => "FreeBSD"
      case 10 => "Compaq TRU64 UNIX"
      case 11 => "Novel Modesto"
      case 12 => "OpenBSD"
      case 64 => "ARM EABI"
      case 97 => "ARM"
      case 255 => "Standalone (embedded) application"
      case _ => "Invalid"
    }
    
    private val typeStrings: Int => String = {
      case 0 => "NONE (No file type)"
      case 1 => "REL (Relocatable file)"
      case 2 => "EXEC (Executable file)"
      case 4 => "DYN (Shared object file)"
      case 5 => "CORE (Core file)"
      case _ => "Unknown"
    }
    
    private val machineStrings: Int => String = {
      case 0 => "No machine"
      case 3 => "Intel 80386"
      case 62 => "AMD x86-64 architecture"
      case _ => "Unknown"
    }
    
    override val toString = List("Magic: " + e_indent.toHex,
        "Class:                             " + classStrings(e_indent.get(4)),
        "Data:                              " + dataStrings(e_indent.get(5)),
        "Version:                           " + elfVersionStrings(e_indent.get(6)),
        "OS/ABI:                            " + osAbiStrings(e_indent.get(7)),
        "ABI Version:                       " + e_indent.get(8),
        "Type:                              " + typeStrings(e_type),
        "Machine:                           " + machineStrings(e_machine),
        "Version:                           " + elfVersionStrings(e_version.toInt),
        "Entry Point Address:               0x" + toHex(e_entry),
        "Start of program headers:          " + e_phoff,
        "Start of section headers:          " + e_shoff,
        "Flags:                             0x" + toHex(e_flags),
        "Size of this header:               " + e_ehsize,
        "Size of program headers:           " + e_phentsize,
        "Number of program headers:         " + e_phnum,
        "Size of section headers:           " + e_shentsize,
        "Number of section headers:         " + e_shnum,
        "Section header string table index: " + e_shstrndx
      ).mkString("\n") 
  }
  
  /**
   * typedef struct {
               uint32_t   p_type;
               Elf32_Off  p_offset;
               Elf32_Addr p_vaddr;
               Elf32_Addr p_paddr;
               uint32_t   p_filesz;
               uint32_t   p_memsz;
               uint32_t   p_flags;
               uint32_t   p_align;
           } Elf32_Phdr;
   * 
   */
  class ProgramHeader(bytes: ByteVector) {
    val size = progHeaderSize
    
    private val uint32_t = if (identifier.get(5) == 1) uint32L else uint32
    
    val codec = 
      uint32_t ~ // p_type 
      uint32_t ~ // p_offset
      uint32_t ~ // p_vaddr
      uint32_t ~ // p_paddr
      uint32_t ~ // p_filesz
      uint32_t ~ // p_memsz
      uint32_t ~ // p_flags
      uint32_t   // p_align
      
    val (((((((
        p_type,
        p_offset),
        p_vaddr),
        p_paddr),
        p_filesz),
        p_memsz),
        p_flags),
        p_align) = codec.decode(bytes.bits) match {
      case Attempt.Successful(DecodeResult(list, _)) => list
      case Attempt.Failure(cause) => throw new IllegalArgumentException(cause.messageWithContext)
    }
  }
  
  /**
   * typedef struct {
               uint32_t   sh_name;
               uint32_t   sh_type;
               uint32_t   sh_flags;
               Elf32_Addr sh_addr;
               Elf32_Off  sh_offset;
               uint32_t   sh_size;
               uint32_t   sh_link;
               uint32_t   sh_info;
               uint32_t   sh_addralign;
               uint32_t   sh_entsize;
           } Elf32_Shdr;
   * 
   */
  class SectionHeader(bytes: ByteVector) {
    val size = secHeaderSize
    
    private val uint32_t = if (identifier.get(5) == 1) uint32L else uint32

    val codec = 
      uint32_t ~ // sh_name
      uint32_t ~ // sh_type
      uint32_t ~ // sh_flags
      uint32_t ~ // sh_addr
      uint32_t ~ // sh_offset
      uint32_t ~ // sh_size
      uint32_t ~ // sh_link
      uint32_t ~ // sh_info
      uint32_t ~ // ash_addralign
      uint32_t   // sh_entsize
      
    val (((((((((
        sh_name,
        sh_type),
        sh_flags),
        sh_addr),
        sh_offset),
        sh_size),
        sh_link),
        sh_info),
        sh_addralign),
        sh_entsize) = codec.decode(bytes.bits) match {
      case Attempt.Successful(DecodeResult(list, _)) => list
      case Attempt.Failure(cause) => throw new IllegalArgumentException(cause.messageWithContext)
    }
  }
     
  val stream = input
  
  require(input.length >= headerSize, "Not a vaild ELF file: insufficient bytes.")
  
  val identifier = input.take(identifierSize)
  
  require(identifier.slice(0, 4) == hex"7f454c46", "Not a valide ELF file: invalid magic string.")
  require(identifier.get(4) == 1, "Only 32-bit ELF files are supported.")
      
  val header = new Header(identifier, input.slice(identifierSize, headerSize))
  
  private def parseMany[A](bytes: ByteVector, start: Int, step: Int, num: Int,
      constructor: ByteVector => A, result: Vector[A]) : Vector[A] =
      if (num == 0) result
      else {
        parseMany[A](bytes, start + step, step, num - 1,
            constructor, result :+ constructor(bytes.slice(start, start + step)))
      }
    
  val progHeaders : Vector[ProgramHeader] =
    parseMany(stream, header.e_phoff.toInt, progHeaderSize, header.e_phnum, 
        bytes => new ProgramHeader(bytes), Vector.empty)
  
  val secHeaders : Vector[SectionHeader] = 
    parseMany(stream, header.e_shoff.toInt, secHeaderSize, header.e_shnum,
        bytes => new SectionHeader(bytes), Vector.empty)

  def withinValidRange(imm : Long) : Boolean = {
    def eachSec(headers : Vector[SectionHeader]) : Boolean = headers match {
      case xs :+ x => (x.sh_addr <= imm && x.sh_addr + x.size >= imm) || eachSec(xs)
      case _ => false
    }
    eachSec(secHeaders)
  }

}