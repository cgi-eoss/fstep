class base {
  # no-op class
}

node default {
  include(lookup('classes', { 'merge' => 'unique', 'default_value' => [] }))
}
