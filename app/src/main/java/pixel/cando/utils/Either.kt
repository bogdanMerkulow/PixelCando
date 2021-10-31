package pixel.cando.utils

sealed class Either<A, B> {
    class Left<A, B>(val left: A) : Either<A, B>()
    class Right<A, B>(val right: B) : Either<A, B>()
}

val <A, B> Either<A, B>.left: A?
    get() = if (this is Either.Left) this.left else null

val <A, B> Either<A, B>.right: B?
    get() = if (this is Either.Right) this.right else null

fun <A, B> Either<A, B>.onLeft(
    action: (A) -> Unit
) = left?.let(action)

fun <A, B> Either<A, B>.onRight(
    action: (B) -> Unit
) = right?.let(action)

fun <A, B, A1> Either<A, B>.mapOnlyLeft(
    mapper: (A) -> A1
): Either<A1, B> = when (this) {
    is Either.Left -> Either.Left(mapper.invoke(this.left))
    is Either.Right -> Either.Right(this.right)
}

fun <A, B, A1> Either<A, B>.mapFromLeft(
    mapper: (A) -> Either<A1, B>
): Either<A1, B> = when (this) {
    is Either.Left -> mapper.invoke(this.left)
    is Either.Right -> Either.Right(this.right)
}

fun <A, B, B1> Either<A, B>.mapOnlyRight(
    mapper: (B) -> B1
): Either<A, B1> = when (this) {
    is Either.Right -> Either.Right(mapper.invoke(this.right))
    is Either.Left -> Either.Left(this.left)
}

fun <A, B, B1> Either<A, B>.mapFromRight(
    mapper: (B) -> Either<A, B1>
): Either<A, B1> = when (this) {
    is Either.Left -> Either.Left(this.left)
    is Either.Right -> mapper.invoke(this.right)
}

fun <P, A, B> P.map(
    mapper: (P) -> Either<A, B>
) = mapper.invoke(this)