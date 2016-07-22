package edu.psu.ist.plato.kaiming.exception

class UnreachableCodeException(message: String = null, cause: Throwable = null) extends RuntimeException(message, cause)
class ParsingException(message: String) extends RuntimeException(message)
class UnsupportedLanguageException(message: String) extends RuntimeException(message)
