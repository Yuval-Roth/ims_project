FROM python:3.11

COPY app.py /manager/app.py

RUN pip install flask

EXPOSE 80

ENTRYPOINT ["flask", "--app", "/manager/app", "run", "--port", "80", "--host", "0.0.0.0"]