while cond: #Noncompliant
    break;

def foo(cond, cond2):

    while cond:
        print 42

    while cond:
        continue

    while cond: # Noncompliant [[secondary=+2]]
#   ^^^^^
        break

    while cond:
        if cond2:
            break

    while cond: # Noncompliant [[secondary=+2,+4]]
        if cond2:
            break
        else:
            break

   while cond: # Noncompliant [[secondary=+1]]
       raise error

def try_statements():

    while cond:
        try:
            return doSomething()
        except:
            print("Try again...")

    while cond: # false negative
        try:
            return doSomething()
        except:
            return 42

    while cond:
        try:
            raise error
        except Error as e:
            print(e)

def invalid_continue():
    continue
