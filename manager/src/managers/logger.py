#logger.py
import logging
from logging.handlers import TimedRotatingFileHandler
from datetime import datetime
import os
import sys


def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


class Logger:
    _instance = None
    _print_counter = 0  # Counter for tracking the number of prints

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(Logger, cls).__new__(cls)
            cls._instance._initialize_logger()
        return cls._instance

    @classmethod
    def get_handler(cls):
        return cls().logger.handlers[0]

    def _initialize_logger(self):
        # Set up the logger
        self.logger = logging.getLogger("MyLogger")
        self.logger.setLevel(logging.DEBUG)

        # Create the Logs directory if it doesn't exist
        # path = /app/data/logs
        log_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "data", "logs")
        if not os.path.exists(log_dir):
            os.makedirs(log_dir)

        # Set up the handler for daily log rotation without date in the initial file name
        log_filename = os.path.join(log_dir, "output.log")
        handler = TimedRotatingFileHandler(log_filename, when="midnight", interval=1, backupCount=5)
        handler.suffix = "%Y-%m-%d"  # Use date suffix for rotated files
        handler.setLevel(logging.DEBUG)

        # Create a formatter and set it for the handler
        formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
        handler.setFormatter(formatter)

        # Add the handler to the logger
        self.logger.addHandler(handler)

    @classmethod
    def _pretty_print(cls, message, is_error=False):
        """
        Prints the message and inserts a newline after every 5 messages.
        """
        cls._print_counter += 1

        # Print the message to the appropriate stream
        if is_error:
            eprint(message)
        else:
            print(message)

        # Add a newline every 5 prints
        if cls._print_counter % 5 == 0:
            print("\n")  # Insert a new line after every 5th message


    @classmethod
    def log_debug(cls, message):
        cls._pretty_print(message)
        cls().logger.debug(message)

    @classmethod
    def log_info(cls, message):
        cls._pretty_print(message)
        cls().logger.info(message)

    @classmethod
    def log_warning(cls, message):
        cls._pretty_print(message)
        cls().logger.warning(message)

    @classmethod
    def log_error(cls, message):
        cls._pretty_print(message, is_error=True)
        cls().logger.error(message)

    @classmethod
    def log_critical(cls, message):
        cls._pretty_print(message)
        cls().logger.critical(message)