package utils

object Let {
    def let[A,B](a:A)(f:A=>B):B = f(a)
}