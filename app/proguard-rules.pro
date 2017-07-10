# The simpliest strategy is to not run proguard against your project's own code.
# This doesn't provide the benefits of optimization & obfuscation against your
# project, but will still strip the libraries. The advantage is that your app will
# work without any subsequent effort. If you choose this strategy, the proguard
# configuration for the project is simply the line below.

# -keep class net.named_data.nfd.** { * ; }
# -keep class net.named_data.jndn.** { * ; }
