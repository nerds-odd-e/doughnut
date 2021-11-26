import 'package:flutter/material.dart';

FutureBuilder<String> BazaarWidget(Future<String> futureTitle) {
  return FutureBuilder<String>(
          future: futureTitle,
          builder: (context, snapshot) {
              if (snapshot.hasData) {
                return Text(snapshot.data!, textDirection: TextDirection.ltr);
              } else if (snapshot.hasError) {
                return Text('${snapshot.error}');
              }

              // By default, show a loading spinner.
              return const CircularProgressIndicator();
          },
        );
}

