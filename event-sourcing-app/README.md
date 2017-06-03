This branch contains work up to chapter 2: Handle Simple Events

On a high level, it includes:  
1. everything following scala-web-app project
2. user login
3. add/delete tags - server responds to every activity with a view of current tags in memory
4. tags go into both memory and postgres db in a form of logRecord
5. tags will be reproduced and loaded into memory every time the server is restarted by replaying logs in postgres
