package com.futurenotfound.appserve

sealed abstract class CommandLineItem(name: String, description: String)
case class CommandLineArgument(name: String, description: String) extends CommandLineItem(name, description)
case class CommandLineOption(name: String, description: String, optionName: String) extends CommandLineItem(name, description)

case class PassedInItems(options: Map[String, String] = Map(), arguments: List[String] = List(), hangingOptionName: Option[String] = None)

case class CommandLineOptions(commandLineOptions: Seq[CommandLineOption], commandLineArguments: Seq[CommandLineArgument]) {
  def parse(args: Array[String]): Either[PassedInItems, String] = {
    args.foldLeft(Left(PassedInItems()).asInstanceOf[Either[PassedInItems, String]]){(workingResult, arg) => workingResult match {
      case Left(PassedInItems(options, arguments, hangingOptionName)) => {
        val passedIn = workingResult.left.get
        hangingOptionName match {
          case Some(hangingName) => Left(passedIn.copy(options = options + (hangingName -> arg), hangingOptionName = None))
          case None => {
            if (arg.startsWith("--")) {
              val argumentName = arg.tail.tail
              if (commandLineOptions.exists(option => option.optionName == argumentName)) {
                Left(passedIn.copy(hangingOptionName = Some(argumentName)))
              } else {
                Right("Unexpected argument %s.".format(argumentName))
              }
            }
            else Left(passedIn.copy(arguments = passedIn.arguments :+ arg))
          }
        }
      }
      case default => default
    }}
  }
}

object CommandLineParser {
  def run(args: Array[String],
          commandLineOptions: Seq[CommandLineOption],
          commandLineArguments: Seq[CommandLineArgument],
          success: (Map[String, String], Seq[String]) => Unit): Unit = {
    val parsed = new CommandLineOptions(commandLineOptions, commandLineArguments).parse(args)
    parsed match {
      case Left(passedInItems) => success(passedInItems.options, passedInItems.arguments)
      case Right(errorText) => println(errorText)
    }
  }
}